package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.*

data class RutaLocal(val id: Long,val fecha: String,val cerrada: Boolean,val ultimaSync: String)
data class Registro(val pesaje: Pesaje,val rutaId: Long,val clave: String,val operacion: String,val observacion: String?,val error: String?)
data class FormularioPesaje(val puntoId: Long,val material: String,val pesoBruto: Double,val tara: Double,val observacion: String?)
data class ResumenSync(val enviados: Int,val recibidos: Int)
interface Reloj { fun ahoraIso(): String; fun claveUnica(): String }
interface EcoRepository {
    fun puntos(): Flow<List<PuntoAcopio>>
    fun pesajes(): Flow<List<Registro>>
    fun ruta(): Flow<RutaLocal?>
    suspend fun registrar(formulario: FormularioPesaje,id: Long?=null)
    suspend fun anular(id: Long,motivo: String)
    suspend fun sincronizar(): ResumenSync
    suspend fun cerrarRuta()
    suspend fun omitir(punto: Long,motivo: String)
    suspend fun descartarPendiente(id: Long)
    suspend fun cerrarSesion()
}
