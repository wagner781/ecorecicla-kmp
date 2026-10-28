package pe.upeu.ecorecicla

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.*
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.repository.EcoRepositoryOffline
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*

class HumedadClienteTest {
    private val reloj=object : Reloj { override fun ahoraIso()="2026-10-28T08:00:00-05:00"; override fun claveUnica()="humedad" }
    @Test fun humedadPersisteOfflineSeEnviaYVuelveSinDuplicarse()=runTest {
        val driver=driverMemoria(); val local=BaseLocal(EcoDatabase(driver))
        local.q.guardarRuta(1,"2026-10-28",0,"")
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","operario1","OPERARIO",setOf("PESAJE_CREAR")),3600) }
        var llamadas=0
        val definitivo="""{"id":101,"rutaId":1,"puntoId":1,"fechaHora":"2026-10-28T08:00:00-05:00","material":"Papel","pesoBruto":12,"tara":2,"pesoNeto":10,"operario":"operario1","humedadEstimada":12.75}"""
        fun pagina(c: String)="""{"contenido":[$c],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}"""
        val http=crearCliente(MockEngine { req ->
            llamadas++
            val cuerpo=when {
                req.method==HttpMethod.Post -> {
                    assertEquals(1,llamadas)
                    val json=Json.parseToJsonElement((req.body as TextContent).text).jsonObject
                    assertEquals(12.75,json.getValue("humedadEstimada").jsonPrimitive.double)
                    assertFalse("pesoNeto" in json); definitivo
                }
                req.url.encodedPath.endsWith("/puntos") -> pagina("""{"id":1,"razonSocial":"Empresa","direccion":"Av 1","zona":"Norte","materialHabitual":"Papel"}""")
                req.url.encodedPath.endsWith("/rutas/1") -> """{"id":1,"fecha":"2026-10-28","usuarioId":1,"cerrada":false,"puntos":[]}"""
                req.url.encodedPath.endsWith("/rutas") -> pagina("")
                else -> pagina(definitivo)
            }
            respond(cuerpo,if(req.method==HttpMethod.Post) HttpStatusCode.Created else HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val repo=EcoRepositoryOffline(local,EcoApi(http),seguro,reloj)
            repo.registrar(FormularioPesaje(1,"Papel",12.0,2.0,null,12.75))
            assertEquals(0,llamadas); assertEquals(12.75,local.q.pendientes().executeAsOne().humedadEstimada)
            repo.sincronizar()
            val fila=local.q.pesajes().executeAsOne(); assertEquals(101L,fila.id); assertEquals(12.75,fila.humedadEstimada); assertEquals(0L,fila.pendiente)
        } finally { http.close(); driver.close() }
    }
    @Test fun validaHumedadAntesDeGuardarSinDescartarCamposOpcionales()=runTest {
        val driver=driverMemoria(); val local=BaseLocal(EcoDatabase(driver)); local.q.guardarRuta(1,"2026-10-28",0,"")
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","operario1","OPERARIO",setOf("PESAJE_CREAR")),3600) }
        val http=crearCliente(MockEngine { error("Sin red") },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val repo=EcoRepositoryOffline(local,EcoApi(http),seguro,reloj)
            for(h in listOf(-1.0,100.01,1.123,Double.NaN)) {
                val error=assertFailsWith<ErrorApi.Validacion> { repo.registrar(FormularioPesaje(1,"Papel",12.0,2.0,null,h)) }
                assertTrue("humedadEstimada" in error.campos)
            }
            for(h in listOf(0.0,100.0,null)) repo.registrar(FormularioPesaje(1,"Papel",12.0,2.0,null,h))
            assertEquals(3,local.q.pendientes().executeAsList().size)
        } finally { http.close(); driver.close() }
    }
}
