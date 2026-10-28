package pe.upeu.ecorecicla.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.call.body
import pe.upeu.ecorecicla.data.remote.dto.*

class EcoApi(private val http: HttpClient) {
    private suspend inline fun <reified T> todas(path: String,fecha: String?=null): List<T> {
        val result=mutableListOf<T>(); var pagina=0
        do {
            val page=http.get(path) { parameter("pagina",pagina); parameter("tamanio",100); fecha?.let { parameter("fecha",it) } }.body<PaginaDto<T>>()
            result.addAll(page.contenido); pagina++
        } while(!page.ultima)
        return result
    }
    suspend fun login(usuario: String,password: String): SesionDto = llamada { http.post("auth/login") { setBody(LoginDto(usuario,password)) }.body() }
    suspend fun me(): UsuarioDto = llamada { http.get("me").body() }
    suspend fun ruta(): RutaDto = llamada { http.get("rutas/dia").body() }
    suspend fun ruta(id: Long): RutaDto = llamada { http.get("rutas/$id").body() }
    suspend fun puntos(): List<PuntoDto> = llamada { todas("puntos") }
    suspend fun rutas(fecha: String): List<RutaResumenDto> = llamada { todas("rutas",fecha) }
    suspend fun pesajes(punto: Long): List<PesajeDto> = llamada { todas("puntos/$punto/pesajes") }
    suspend fun crear(body: PesajePeticionDto): PesajeDto = llamada { http.post("pesajes") { setBody(body) }.body() }
    suspend fun corregir(id: Long,body: PesajePeticionDto): PesajeDto = llamada { http.put("pesajes/$id") { setBody(body) }.body() }
    suspend fun anular(id: Long,motivo: String) = llamada { http.delete("pesajes/$id") { setBody(MotivoDto(motivo)) }; Unit }
    suspend fun cerrar(id: Long): RutaDto = llamada { http.post("rutas/$id/cierre").body() }
    suspend fun omitir(ruta: Long,punto: Long,motivo: String) = llamada { http.put("rutas/$ruta/puntos/$punto/omision") { setBody(MotivoDto(motivo)) }; Unit }
}
