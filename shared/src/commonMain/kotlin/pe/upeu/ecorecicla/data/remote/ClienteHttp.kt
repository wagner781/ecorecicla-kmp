package pe.upeu.ecorecicla.data.remote

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.remote.dto.ErrorDto
import pe.upeu.ecorecicla.domain.model.ErrorApi

data class ApiConfig(val baseUrl: String)

fun crearCliente(engine: HttpClientEngine, config: ApiConfig, seguro: AlmacenSeguro, alExpirar: () -> Unit = {}): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys=true }) }
    install(HttpTimeout) { requestTimeoutMillis=15000; connectTimeoutMillis=10000; socketTimeoutMillis=15000 }
    defaultRequest {
        url(config.baseUrl)
        contentType(ContentType.Application.Json)
        seguro.leer()?.let { bearerAuth(it.token) }
    }
    HttpResponseValidator {
        validateResponse { response ->
            if(response.status.value>=400) {
                val error=try { response.body<ErrorDto>() } catch (_: Exception) { ErrorDto(status=response.status.value) }
                val esLogin=response.call.request.url.encodedPath.endsWith("/auth/login")
                if(response.status.value==401 && !esLogin) { seguro.borrar(); alExpirar() }
                throw mapearError(response.status.value,error,esLogin)
            }
        }
    }
}

fun mapearError(status: Int, dto: ErrorDto, esLogin: Boolean=false): ErrorApi = when(status) {
    400 -> ErrorApi.Validacion(dto.validationErrors,dto.message)
    401 -> if(esLogin) ErrorApi.CredencialesIncorrectas(dto.message) else ErrorApi.NoAutorizado()
    403 -> ErrorApi.SinPrivilegio(dto.message)
    404 -> ErrorApi.NoEncontrado(dto.message)
    409 -> ErrorApi.Conflicto(dto.message)
    else -> ErrorApi.Servidor()
}

suspend fun <T> llamada(block: suspend () -> T): T = try { block() }
catch(e: kotlinx.coroutines.CancellationException) { throw e }
catch(e: ErrorApi) { throw e }
catch(e: HttpRequestTimeoutException) { throw ErrorApi.TiempoAgotado() }
catch(e: io.ktor.client.network.sockets.ConnectTimeoutException) { throw ErrorApi.TiempoAgotado() }
catch(e: io.ktor.client.network.sockets.SocketTimeoutException) { throw ErrorApi.TiempoAgotado() }
catch(e: kotlinx.serialization.SerializationException) { throw ErrorApi.Servidor() }
catch(e: Exception) { throw ErrorApi.SinConexion() }
