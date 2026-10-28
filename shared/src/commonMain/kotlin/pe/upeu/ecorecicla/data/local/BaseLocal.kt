package pe.upeu.ecorecicla.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.db.PesajeLocal
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.data.remote.dto.*

class BaseLocal(val db: EcoDatabase) {
    val q=db.ecoQueries
    fun puntos()=q.puntos().asFlow().mapToList(Dispatchers.Default).map { list -> list.map { PuntoAcopio(it.id,it.razonSocial,it.direccion,it.zona,it.materialHabitual,it.secuencia.toInt(),it.atendido,it.motivoOmision) } }
    fun padron()=q.padron().asFlow().mapToList(Dispatchers.Default).map { list -> list.map { PuntoAcopio(it.id,it.razonSocial,it.direccion,it.zona,it.materialHabitual,0,false) } }
    fun rutas()=q.rutasDisponibles().asFlow().mapToList(Dispatchers.Default).map { list -> list.map { RutaResumen(it.id,it.fecha,it.usuarioId,it.usuario,it.cerrada==1L) } }
    fun pesajes()=q.pesajes().asFlow().mapToList(Dispatchers.Default).map { list -> list.map { it.dominio() } }
    fun ruta()=q.ruta().asFlow().mapToOneOrNull(Dispatchers.Default).map { it?.let { RutaLocal(it.id,it.fecha,it.cerrada==1L,it.ultimaSync,it.usuarioId) } }
    fun guardar(r: Registro) {
        val p=r.pesaje; val estado=p.estado as? EstadoPesaje.Anulado
        q.guardarPesaje(p.id,p.puntoId,r.rutaId,p.fechaHora,p.material,p.pesoBruto,p.tara,p.pesoNeto,p.operario,if(estado==null) 0 else 1,estado?.motivo,if(p.pendiente) 1 else 0,r.clave,r.operacion,r.observacion,r.error,p.humedadEstimada)
    }
    fun definitivo(dto: PesajeDto,clave: String="servidor-${dto.id}")=guardar(Registro(dto.dominio(),dto.rutaId,clave,"",dto.observacion,null))
    fun limpiar()=db.transaction { q.borrarPesajes(); q.borrarPuntos(); q.borrarRuta(); q.borrarPadron(); q.borrarRutasDisponibles() }
}

fun PesajeLocal.dominio()=Registro(Pesaje(id,puntoId,fechaHora,material,pesoBruto,tara,pesoNeto,operario,if(anulado==1L) EstadoPesaje.Anulado(motivo.orEmpty()) else EstadoPesaje.Registrado,pendiente==1L,humedadEstimada),rutaId,clave,operacion,observacion,error)
