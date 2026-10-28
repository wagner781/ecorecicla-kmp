package pe.upeu.ecorecicla

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.repository.*
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.*

class SesionTest {
    @Test fun perfilNoRenuevaExpiracionNiGuardaDeNuevoElToken()=runTest {
        var escrituras=0
        val original=Sesion("jwt","operario1","OPERARIO",setOf("PUNTO_LEER"))
        val seguro=object : AlmacenSeguro {
            override fun leer()=original
            override fun guardar(sesion: Sesion,expiresIn: Long) { escrituras++ }
            override fun borrar() {}
        }
        val driver=driverMemoria()
        val reloj=object : Reloj { override fun ahoraIso()="2026-10-28T08:00:00-05:00"; override fun claveUnica()="prueba" }
        val http=crearCliente(MockEngine { respond("""{"id":1,"usuario":"operario1","nombre":"Operario","rol":"OPERARIO","privilegios":["PUNTO_LEER"]}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json")) },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val api=EcoApi(http)
            val datos=EcoRepositoryOffline(BaseLocal(EcoDatabase(driver)),api,seguro,reloj)
            val perfil=ConsultarSesionUseCase(SesionRepositoryImpl(api,seguro,datos))()
            assertEquals(original.token,perfil.token); assertEquals(0,escrituras)
        } finally { http.close(); driver.close() }
    }
    @Test fun loginFallidoNoSeInterpretaComoUnaSesionVencida()=runTest {
        val seguro=SeguroMemoria()
        val driver=driverMemoria()
        val reloj=object : Reloj { override fun ahoraIso()="2026-10-28T08:00:00-05:00"; override fun claveUnica()="prueba" }
        val http=crearCliente(MockEngine { respond("""{"status":401,"message":"Credenciales incorrectas"}""",HttpStatusCode.Unauthorized,headersOf(HttpHeaders.ContentType,"application/json")) },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val api=EcoApi(http)
            val datos=EcoRepositoryOffline(BaseLocal(EcoDatabase(driver)),api,seguro,reloj)
            val caso=IniciarSesionUseCase(SesionRepositoryImpl(api,seguro,datos))
            assertFailsWith<ErrorApi.CredencialesIncorrectas> { caso("operario1","incorrecta") }
            assertNull(seguro.leer())
        } finally { http.close(); driver.close() }
    }
}
