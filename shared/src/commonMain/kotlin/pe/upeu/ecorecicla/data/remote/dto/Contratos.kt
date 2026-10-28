package pe.upeu.ecorecicla.data.remote.dto

import kotlinx.serialization.Serializable
import pe.upeu.ecorecicla.domain.model.*

@Serializable data class LoginDto(val usuario: String,val password: String)
@Serializable data class SesionDto(val token: String,val usuario: String,val nombre: String,val rol: String,val privilegios: Set<String>,val expiresIn: Long=3600,val id: Long=0,val expiresAt: Long?=null) {
    fun dominio()=Sesion(token,usuario,rol,privilegios,id,nombre,expiresAt)
}
@Serializable data class UsuarioDto(val id: Long,val usuario: String,val nombre: String,val rol: String,val privilegios: Set<String>)
@Serializable data class PaginaDto<T>(val contenido: List<T>,val pagina: Int,val tamanio: Int,val totalElementos: Long,val totalPaginas: Int,val ultima: Boolean)
@Serializable data class ErrorDto(val status: Int=500,val message: String="Error del servicio",val validationErrors: Map<String,String> = emptyMap())
@Serializable data class PuntoDto(val id: Long,val razonSocial: String,val direccion: String,val zona: String,val materialHabitual: String,val secuencia: Int=0,val atendido: Boolean=false,val motivoOmision: String?=null) {
    fun dominio()=PuntoAcopio(id,razonSocial,direccion,zona,materialHabitual,secuencia,atendido,motivoOmision)
}
@Serializable data class RutaDto(val id: Long,val fecha: String,val usuarioId: Long,val cerrada: Boolean,val puntos: List<PuntoDto>)
@Serializable data class PesajeDto(val id: Long,val rutaId: Long,val puntoId: Long,val fechaHora: String,val material: String,val pesoBruto: Double,val tara: Double,val pesoNeto: Double,val operario: String,val operarioId: Long=0,val observacion: String?=null,val anulado: Boolean=false,val motivoAnulacion: String?=null,val humedadEstimada: Double?=null) {
    fun dominio()=Pesaje(id,puntoId,fechaHora,material,pesoBruto,tara,pesoNeto,operario,if(anulado) EstadoPesaje.Anulado(motivoAnulacion.orEmpty()) else EstadoPesaje.Registrado,false,humedadEstimada)
}
// El cliente nunca envia pesoNeto.
@Serializable data class PesajePeticionDto(val rutaId: Long,val puntoId: Long,val material: String,val pesoBruto: Double,val tara: Double,val observacion: String?,val claveCliente: String,val humedadEstimada: Double?=null)
@Serializable data class MotivoDto(val motivo: String)
@Serializable data class RutaResumenDto(val id: Long,val fecha: String,val usuarioId: Long,val usuario: String,val cerrada: Boolean) {
    fun dominio()=RutaResumen(id,fecha,usuarioId,usuario,cerrada)
}
