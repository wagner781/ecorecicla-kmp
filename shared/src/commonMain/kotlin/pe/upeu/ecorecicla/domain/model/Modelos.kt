package pe.upeu.ecorecicla.domain.model

data class PuntoAcopio(val id: Long, val razonSocial: String, val direccion: String, val zona: String, val materialHabitual: String, val secuencia: Int, val atendido: Boolean,val motivoOmision: String?=null)
sealed interface EstadoPesaje {
    data object Registrado : EstadoPesaje
    data class Anulado(val motivo: String) : EstadoPesaje
}
data class Pesaje(val id: Long, val puntoId: Long, val fechaHora: String, val material: String, val pesoBruto: Double, val tara: Double, val pesoNeto: Double, val operario: String, val estado: EstadoPesaje, val pendiente: Boolean)
data class Sesion(val token: String, val usuario: String, val rol: String, val privilegios: Set<String>,val id: Long=0,val nombre: String=usuario,val expiresAt: Long?=null)

val Sesion.soloLectura: Boolean
    get()=privilegios.isNotEmpty() && privilegios.all { it=="PUNTO_LEER" || it=="PESAJE_LEER" }
