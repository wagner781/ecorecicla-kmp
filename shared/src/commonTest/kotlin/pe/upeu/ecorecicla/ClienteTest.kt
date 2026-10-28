package pe.upeu.ecorecicla

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.remote.dto.*
import pe.upeu.ecorecicla.domain.model.*

class SeguroMemoria : AlmacenSeguro {
    var sesion: Sesion?=null
    override fun leer()=sesion
    override fun guardar(sesion: Sesion,expiresIn: Long) { this.sesion=sesion }
    override fun borrar() { sesion=null }
}
class ClienteTest {
    @Test fun convierteDtoSinAnotacionesEnDominio() {
        val p=PesajeDto(1,2,3,"2026-10-28T08:00:00-05:00","Papel",12.0,2.0,10.0,"operario1",anulado=true,motivoAnulacion="Duplicado").dominio()
        assertEquals(EstadoPesaje.Anulado("Duplicado"),p.estado); assertFalse(p.pendiente)
    }
    @Test fun traduce403YConservaSesion()=runTest {
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","op","OPERARIO",emptySet()),3600) }
        val engine=MockEngine { req ->
            assertEquals("Bearer jwt",req.headers[HttpHeaders.Authorization])
            respond("""{"status":403,"message":"Sin privilegio"}""",HttpStatusCode.Forbidden,headersOf(HttpHeaders.ContentType,"application/json"))
        }
        val client=crearCliente(engine,ApiConfig("https://eco.test/api/v1/"),seguro)
        try { assertFailsWith<ErrorApi.SinPrivilegio> { EcoApi(client).me() }; assertNotNull(seguro.leer()) } finally { client.close() }
    }
    @Test fun borraSesionAlRecibir401()=runTest {
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","op","OPERARIO",emptySet()),3600) }
        val client=crearCliente(MockEngine { respond("{}",HttpStatusCode.Unauthorized,headersOf(HttpHeaders.ContentType,"application/json")) },ApiConfig("https://eco.test/api/v1/"),seguro)
        try { assertFailsWith<ErrorApi.NoAutorizado> { EcoApi(client).me() }; assertNull(seguro.leer()) } finally { client.close() }
    }
}
