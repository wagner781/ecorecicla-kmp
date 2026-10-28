package pe.upeu.ecorecicla

import app.cash.sqldelight.db.SqlDriver
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.repository.EcoRepositoryOffline
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*

expect fun driverMemoria(): SqlDriver

class OfflineTest {
    private val reloj=object : Reloj {
        var secuencia=0
        override fun ahoraIso()="2026-10-28T08:00:00-05:00"
        override fun claveUnica()="dispositivo-prueba-${++secuencia}"
    }
    private fun local(driver: SqlDriver)=BaseLocal(EcoDatabase(driver)).apply {
        q.guardarRuta(1,"2026-10-28",0,"sin sincronizar")
        q.guardarPunto(1,"Empresa","Direccion","Norte","Papel",1,null)
    }
    private fun seguro()=SeguroMemoria().apply { guardar(Sesion("jwt","operario1","OPERARIO",setOf("PESAJE_CREAR","PESAJE_EDITAR")),3600) }
    @Test fun registraSinRedConIdsNegativosUnicos()=runTest {
        val driver=driverMemoria(); val local=local(driver)
        var llamadas=0
        val http=crearCliente(MockEngine { llamadas++; error("No debe consultar la red") },ApiConfig("https://eco.test/api/v1/"),seguro())
        val repo=EcoRepositoryOffline(local,EcoApi(http),seguro(),reloj)
        try {
            repeat(2) { repo.registrar(FormularioPesaje(1,"Papel",12.0,2.0,null)) }
            val filas=local.q.pendientes().executeAsList()
            assertEquals(setOf(-1L,-2L),filas.map { it.id }.toSet()); assertTrue(filas.all { it.pendiente==1L }); assertEquals(0,llamadas)
            assertTrue(local.q.puntos().executeAsOne().atendido)
        } finally { http.close(); driver.close() }
    }
    @Test fun enviaAntesDeDescargarYSustituyeIdSinDuplicar()=runTest {
        val driver=driverMemoria(); val local=local(driver); val eventos=mutableListOf<String>()
        val definitivo="""{"id":101,"rutaId":1,"puntoId":1,"fechaHora":"2026-10-28T08:00:00-05:00","material":"Papel","pesoBruto":12.0,"tara":2.0,"pesoNeto":10.0,"operario":"operario1"}"""
        val http=crearCliente(MockEngine { req ->
            eventos.add("${req.method.value} ${req.url.encodedPath}")
            val body=when {
                req.method==HttpMethod.Post -> definitivo
                req.url.encodedPath.endsWith("rutas/dia") -> """{"id":1,"fecha":"2026-10-28","usuarioId":1,"cerrada":false,"puntos":[{"id":1,"razonSocial":"Empresa","direccion":"Direccion","zona":"Norte","materialHabitual":"Papel","secuencia":1,"atendido":true}]}"""
                else -> """{"contenido":[$definitivo],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}"""
            }
            respond(body,if(req.method==HttpMethod.Post) HttpStatusCode.Created else HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        },ApiConfig("https://eco.test/api/v1/"),seguro())
        val repo=EcoRepositoryOffline(local,EcoApi(http),seguro(),reloj)
        try {
            repo.registrar(FormularioPesaje(1,"Papel",12.0,2.0,null))
            assertEquals(ResumenSync(1,1),repo.sincronizar())
            assertEquals(listOf("POST /api/v1/pesajes","GET /api/v1/rutas/dia","GET /api/v1/puntos/1/pesajes"),eventos)
            assertEquals(101L,local.q.pesajes().executeAsOne().id); assertTrue(local.q.pendientes().executeAsList().isEmpty())
            repo.sincronizar(); assertEquals(1,local.q.pesajes().executeAsList().size)
            repo.registrar(FormularioPesaje(1,"Metal",15.0,1.0,null)); assertEquals(-1L,local.q.pendientes().executeAsOne().id)
        } finally { http.close(); driver.close() }
    }
    @Test fun conservaPendienteRechazadoSinDescargar()=runTest {
        val driver=driverMemoria(); val local=local(driver); var llamadas=0
        val http=crearCliente(MockEngine {
            llamadas++; respond("""{"status":409,"message":"La ruta ya esta cerrada"}""",HttpStatusCode.Conflict,headersOf(HttpHeaders.ContentType,"application/json"))
        },ApiConfig("https://eco.test/api/v1/"),seguro())
        val repo=EcoRepositoryOffline(local,EcoApi(http),seguro(),reloj)
        try {
            repo.registrar(FormularioPesaje(1,"Papel",12.0,2.0,null))
            assertFailsWith<ErrorApi.Conflicto> { repo.sincronizar() }
            assertEquals(1,llamadas); assertEquals(-1L,local.q.pendientes().executeAsOne().id)
            assertEquals("La ruta ya esta cerrada",local.q.pendientes().executeAsOne().error)
            repo.descartarPendiente(-1)
            assertTrue(local.q.pendientes().executeAsList().isEmpty()); assertEquals(1,llamadas)
        } finally { http.close(); driver.close() }
    }
    @Test fun cerrarSesionLimpiaDatosDelTurno()=runTest {
        val driver=driverMemoria(); val local=local(driver); val seguro=seguro()
        val http=crearCliente(MockEngine { error("sin red") },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val repo=EcoRepositoryOffline(local,EcoApi(http),seguro,reloj)
            repo.registrar(FormularioPesaje(1,"Papel",12.0,2.0,null)); repo.cerrarSesion()
            assertNull(seguro.leer()); assertNull(local.q.ruta().executeAsOneOrNull()); assertTrue(local.q.pesajes().executeAsList().isEmpty())
        } finally { http.close(); driver.close() }
    }
    @Test fun supervisorCorrigeOtraRutaSinUsarEstadoDeSuRuta()=runTest {
        val driver=driverMemoria(); val local=local(driver)
        local.q.guardarRuta(1,"2026-10-28",1,"sincronizado")
        local.definitivo(pe.upeu.ecorecicla.data.remote.dto.PesajeDto(17,2,1,"2026-10-28T08:00:00-05:00","Papel",20.0,2.0,18.0,"operario1"))
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","supervisor1","SUPERVISOR",setOf("PESAJE_EDITAR","PESAJE_ANULAR")),3600) }
        val http=crearCliente(MockEngine { error("Debe guardar localmente") },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val repo=EcoRepositoryOffline(local,EcoApi(http),seguro,reloj)
            repo.registrar(FormularioPesaje(1,"Papel",25.0,2.0,null),17)
            val fila=local.q.pendientes().executeAsOne()
            assertEquals(2L,fila.rutaId); assertEquals(25.0,fila.pesoBruto); assertEquals(18.0,fila.pesoNeto)
        } finally { http.close(); driver.close() }
    }
}
