package pe.upeu.ecorecicla.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.call.body
import pe.upeu.ecorecicla.data.remote.dto.*

class EcoApi(private val http: HttpClient) {
    suspend fun login(usuario: String,password: String): SesionDto = llamada { http.post("auth/login") { setBody(LoginDto(usuario,password)) }.body() }
    suspend fun me(): UsuarioDto = llamada { http.get("me").body() }
    suspend fun ruta(): RutaDto = llamada { http.get("rutas/dia").body() }
    suspend fun pesajes(punto: Long): List<PesajeDto> = llamada {
        val result=mutableListOf<PesajeDto>(); var pagina=0
        do {
            val page=http.get("puntos/$punto/pesajes") { parameter("pagina",pagina); parameter("tamanio",100) }.body<PaginaDto<PesajeDto>>()
            result.addAll(page.contenido); pagina++
        } while(!page.ultima)
        result
    }
    suspend fun crear(body: PesajePeticionDto): PesajeDto = llamada { http.post("pesajes") { setBody(body) }.body() }
    suspend fun corregir(id: Long,body: PesajePeticionDto): PesajeDto = llamada { http.put("pesajes/$id") { setBody(body) }.body() }
    suspend fun anular(id: Long,motivo: String) = llamada { http.delete("pesajes/$id") { setBody(MotivoDto(motivo)) }; Unit }
    suspend fun cerrar(id: Long): RutaDto = llamada { http.post("rutas/$id/cierre").body() }
    suspend fun omitir(ruta: Long,punto: Long,motivo: String) = llamada { http.put("rutas/$ruta/puntos/$punto/omision") { setBody(MotivoDto(motivo)) }; Unit }
}
