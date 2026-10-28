package pe.upeu.ecorecicla

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.remote.dto.*
import pe.upeu.ecorecicla.data.repository.EcoRepositoryOffline
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.db.EcoDatabase

class CatalogoTest {
    private val reloj=object : Reloj { override fun ahoraIso()="2026-10-28T08:00:00-05:00"; override fun claveUnica()="clave-prueba" }
    private fun pagina(contenido: String)="""{"contenido":[$contenido],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}"""
    private val punto="""{"id":1,"razonSocial":"Empresa","direccion":"Av. Prueba","zona":"Norte","materialHabitual":"Papel"}"""
    private val peso="""{"id":101,"rutaId":1,"puntoId":1,"fechaHora":"2026-10-28T08:00:00-05:00","material":"Papel","pesoBruto":12.0,"tara":2.0,"pesoNeto":10.0,"operario":"operario1"}"""
    @Test fun administradorSinRutaPuedeLeerPadronYPesajesLocales()=runTest {
        val driver=driverMemoria(); val local=BaseLocal(EcoDatabase(driver))
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","admin","ADMINISTRADOR",setOf("PUNTO_LEER","PESAJE_LEER","PUNTO_GESTIONAR")),3600) }
        val http=crearCliente(MockEngine { req ->
            when(req.url.encodedPath) {
                "/api/v1/rutas/dia" -> respond("""{"status":404,"message":"Sin ruta"}""",HttpStatusCode.NotFound,headersOf(HttpHeaders.ContentType,"application/json"))
                "/api/v1/puntos" -> respond(pagina(punto),HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
                "/api/v1/rutas" -> respond(pagina(""),HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
                else -> respond(pagina(peso),HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
            }
        },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val repo=EcoRepositoryOffline(local,EcoApi(http),seguro,reloj)
            assertEquals(ResumenSync(0,1),repo.sincronizar())
            assertNull(repo.ruta().first()); assertEquals("Empresa",repo.padron().first().single().razonSocial)
            assertEquals(101L,repo.pesajes().first().single().pesaje.id)
            assertNotNull(seguro.leer())
        } finally { http.close(); driver.close() }
    }
    @Test fun refrescoDeRuta404NoEnviaNiBorraPendientes()=runTest {
        val driver=driverMemoria(); val local=BaseLocal(EcoDatabase(driver))
        local.q.guardarRuta(1,"2026-10-28",0,"anterior")
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","operario1","OPERARIO",setOf("PESAJE_CREAR")),3600) }
        val metodos=mutableListOf<HttpMethod>()
        val http=crearCliente(MockEngine { req ->
            metodos.add(req.method)
            when(req.url.encodedPath) {
                "/api/v1/rutas/1" -> respond("""{"status":404,"message":"Ruta inexistente"}""",HttpStatusCode.NotFound,headersOf(HttpHeaders.ContentType,"application/json"))
                "/api/v1/puntos" -> respond(pagina(punto),HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
                else -> respond(pagina(""),HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
            }
        },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val repo=EcoRepositoryOffline(local,EcoApi(http),seguro,reloj)
            repo.registrar(FormularioPesaje(1,"Papel",20.0,2.0,null))
            repo.refrescarDatos()
            assertNull(repo.ruta().first()); assertEquals(-1L,local.q.pendientes().executeAsOne().id)
            assertTrue(metodos.all { it==HttpMethod.Get }); assertEquals(1,repo.padron().first().size)
        } finally { http.close(); driver.close() }
    }
    @Test fun refrescoPreservaCorreccionPositivaPendienteContraVersionViejaDelServidor()=runTest {
        val driver=driverMemoria(); val local=BaseLocal(EcoDatabase(driver))
        local.q.guardarRuta(1,"2026-10-28",0,"anterior")
        local.definitivo(PesajeDto(101,1,1,"2026-10-28T08:00:00-05:00","Papel",12.0,2.0,10.0,"operario1"))
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","operario1","OPERARIO",setOf("PESAJE_EDITAR")),3600) }
        val http=crearCliente(MockEngine { req ->
            val body=when(req.url.encodedPath) {
                "/api/v1/puntos" -> pagina(punto)
                "/api/v1/rutas/1" -> """{"id":1,"fecha":"2026-10-28","usuarioId":1,"cerrada":false,"puntos":[$punto]}"""
                "/api/v1/rutas" -> pagina("" )
                else -> pagina(peso)
            }
            respond(body,HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val repo=EcoRepositoryOffline(local,EcoApi(http),seguro,reloj)
            repo.registrar(FormularioPesaje(1,"Papel",30.0,2.0,null),101)
            repo.refrescarDatos()
            assertEquals(30.0,local.q.pendientes().executeAsOne().pesoBruto)
            assertEquals(1,local.q.pesajes().executeAsList().size)
        } finally { http.close(); driver.close() }
    }
    @Test fun cambiarRutaConPendientesSeRechazaAntesDeConsultarLaRed()=runTest {
        val driver=driverMemoria(); val local=BaseLocal(EcoDatabase(driver)); local.q.guardarRuta(1,"2026-10-28",0,"")
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","supervisor1","SUPERVISOR",setOf("PESAJE_CREAR","RUTA_CERRAR")),3600) }
        val http=crearCliente(MockEngine { error("No debe llamar a la red") },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val repo=EcoRepositoryOffline(local,EcoApi(http),seguro,reloj)
            repo.registrar(FormularioPesaje(1,"Papel",20.0,2.0,null))
            assertFailsWith<ErrorApi.Conflicto> { repo.seleccionarRuta(2) }
            assertEquals(1L,local.q.ruta().executeAsOne().id)
        } finally { http.close(); driver.close() }
    }
    @Test fun cierreActualizaDetalleYListaLocalDeRutasSinPerderPropietario()=runTest {
        val driver=driverMemoria(); val local=BaseLocal(EcoDatabase(driver))
        local.q.guardarRuta(1,"2026-10-28",0,""); local.q.asignarUsuarioRuta(7,1)
        local.q.guardarRutaDisponible(1,"2026-10-28",7,"operario1",0)
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","supervisor1","SUPERVISOR",setOf("RUTA_CERRAR")),3600) }
        val http=crearCliente(MockEngine { respond("""{"id":1,"fecha":"2026-10-28","usuarioId":7,"cerrada":true,"puntos":[]}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json")) },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            EcoRepositoryOffline(local,EcoApi(http),seguro,reloj).cerrarRuta()
            assertEquals(1L,local.q.ruta().executeAsOne().cerrada); assertEquals(7L,local.q.ruta().executeAsOne().usuarioId)
            assertEquals(1L,local.q.rutasDisponibles().executeAsOne().cerrada)
        } finally { http.close(); driver.close() }
    }
}
