package pe.upeu.ecorecicla

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.repository.AdministracionRepositoryImpl
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.usecase.AdministracionUseCases

class AdministracionClienteTest {
    @Test fun dominioRechazaGestionSinPrivilegioAntesDeAccederARed()=runTest {
        val sesion=SesionViewModel()
        var llamadas=0
        val http=crearCliente(MockEngine { llamadas++; error("No debe llamar") },ApiConfig("https://eco.test/api/v1/"),SeguroMemoria())
        try {
            val casos=AdministracionUseCases(AdministracionRepositoryImpl(EcoApi(http)),sesion)
            assertFailsWith<ErrorApi.SinPrivilegio> { casos.punto(null,FormularioPunto("Empresa","Direccion","Zona","Papel")) }
            assertEquals(0,llamadas)
        } finally { http.close() }
    }
    @Test fun gestionaSinIntentarDeserializar204YConservaValidacionDelServidor()=runTest {
        val sesion=SesionViewModel().apply { this.sesion.value=Sesion("jwt","admin","ADMINISTRADOR",setOf("PUNTO_GESTIONAR")) }
        val metodos=mutableListOf<HttpMethod>()
        val http=crearCliente(MockEngine { req ->
            metodos.add(req.method)
            if(req.method==HttpMethod.Delete) respond("",HttpStatusCode.NoContent)
            else respond("""{"status":400,"message":"Datos invalidos","validationErrors":{"rol":"El rol no existe"}}""",HttpStatusCode.BadRequest,headersOf(HttpHeaders.ContentType,"application/json"))
        },ApiConfig("https://eco.test/api/v1/"),SeguroMemoria())
        try {
            val casos=AdministracionUseCases(AdministracionRepositoryImpl(EcoApi(http)),sesion)
            casos.eliminarPunto(123)
            val error=assertFailsWith<ErrorApi.Validacion> { casos.usuario(null,FormularioUsuario("nuevo","Nuevo usuario","NO_EXISTE","Eco2026*")) }
            assertEquals("El rol no existe",error.campos["rol"])
            assertEquals(listOf(HttpMethod.Delete,HttpMethod.Post),metodos)
        } finally { http.close() }
    }
    @Test fun listasAdministrativasRecorrenPaginasAntesDeConvertirADominio()=runTest {
        val paginas=mutableListOf<String>()
        val http=crearCliente(MockEngine { req ->
            paginas.add(req.url.encodedPath+":"+req.url.parameters["pagina"])
            val body=when {
                req.url.encodedPath.endsWith("usuarios") && req.url.parameters["pagina"]=="0" -> """{"contenido":[{"id":1,"usuario":"uno","nombre":"Uno","rol":"OPERARIO","privilegios":[],"activo":true}],"pagina":0,"tamanio":100,"totalElementos":2,"totalPaginas":2,"ultima":false}"""
                req.url.encodedPath.endsWith("usuarios") -> """{"contenido":[{"id":2,"usuario":"dos","nombre":"Dos","rol":"OPERARIO","privilegios":[],"activo":false}],"pagina":1,"tamanio":100,"totalElementos":2,"totalPaginas":2,"ultima":true}"""
                else -> """{"contenido":[],"pagina":0,"tamanio":100,"totalElementos":0,"totalPaginas":0,"ultima":true}"""
            }
            respond(body,HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        },ApiConfig("https://eco.test/api/v1/"),SeguroMemoria())
        try {
            val datos=AdministracionRepositoryImpl(EcoApi(http)).maestros()
            assertEquals(listOf("uno","dos"),datos.usuarios.map { it.usuario }); assertFalse(datos.usuarios[1].activo)
            assertTrue(paginas.contains("/api/v1/usuarios:1"))
        } finally { http.close() }
    }
}
