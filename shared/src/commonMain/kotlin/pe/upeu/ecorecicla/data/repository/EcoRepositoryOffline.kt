package pe.upeu.ecorecicla.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.remote.dto.*
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*

class EcoRepositoryOffline(private val local: BaseLocal,private val api: EcoApi,private val seguro: AlmacenSeguro,private val reloj: Reloj) : EcoRepository {
    private val mutex=Mutex()
    private var seleccion: Long?=null
    private var fechaRutas: String?=null
    override fun puntos()=local.puntos()
    override fun pesajes()=local.pesajes()
    override fun ruta()=local.ruta()
    override fun padron()=local.padron()
    override fun rutas()=local.rutas()
    private fun sesion()=seguro.leer() ?: throw ErrorApi.NoAutorizado()
    private fun privilegio(p: String) { if(p !in sesion().privilegios) throw ErrorApi.SinPrivilegio("No posee $p") }
    override suspend fun registrar(formulario: FormularioPesaje,id: Long?) = mutex.withLock {
        privilegio(if(id==null) "PESAJE_CREAR" else "PESAJE_EDITAR")
        val r=local.q.ruta().executeAsOneOrNull() ?: throw ErrorApi.Conflicto("Primero descarga una ruta")
        val anterior=id?.let { actual -> local.q.pesajes().executeAsList().find { it.id==actual }?.dominio() }
        if(r.cerrada==1L && (anterior==null || anterior.rutaId==r.id)) throw ErrorApi.Conflicto("La ruta ya esta cerrada")
        val errores=buildMap {
            formulario.humedadEstimada?.let { h ->
                if(!h.isFinite() || h<0 || h>100 || kotlin.math.round(h*100)/100!=h) put("humedadEstimada","Usa un porcentaje de 0 a 100 con hasta 2 decimales")
            }
            if(formulario.material.isBlank()) put("material","Indica un material")
            if(!formulario.pesoBruto.isFinite() || formulario.pesoBruto<=0) put("pesoBruto","Debe ser mayor que cero")
            if(!formulario.tara.isFinite() || formulario.tara<0 || formulario.tara>=formulario.pesoBruto) put("tara","Debe ser no negativa y menor que el bruto")
        }
        if(errores.isNotEmpty()) throw ErrorApi.Validacion(errores,"Revisa los campos")
        local.db.transaction {
            val previo=anterior
            if(id!=null && previo==null) throw ErrorApi.NoEncontrado("Pesaje inexistente")
            val provisional=id ?: requireNotNull(local.q.siguienteId().executeAsOne().expr)
            val p=Pesaje(provisional,formulario.puntoId,previo?.pesaje?.fechaHora ?: reloj.ahoraIso(),formulario.material,formulario.pesoBruto,formulario.tara,previo?.pesaje?.pesoNeto ?: 0.0,previo?.pesaje?.operario ?: sesion().usuario,EstadoPesaje.Registrado,true,formulario.humedadEstimada)
            local.guardar(Registro(p,previo?.rutaId ?: r.id,previo?.clave ?: reloj.claveUnica(),if(provisional<0) "CREAR" else "EDITAR",formulario.observacion,null))
        }
    }
    override suspend fun anular(id: Long,motivo: String) = mutex.withLock {
        privilegio("PESAJE_ANULAR")
        if(motivo.isBlank()) throw ErrorApi.Validacion(mapOf("motivo" to "Indica el motivo"),"Motivo obligatorio")
        val p=local.q.pesajes().executeAsList().find { it.id==id }?.dominio() ?: throw ErrorApi.NoEncontrado("Pesaje inexistente")
        if(id<0) local.q.borrarPesaje(id) else local.guardar(p.copy(pesaje=p.pesaje.copy(estado=EstadoPesaje.Anulado(motivo),pendiente=true),operacion="ANULAR",error=null))
        Unit
    }
    override suspend fun sincronizar(): ResumenSync = mutex.withLock {
        sesion(); var enviados=0
        // Un error bloquea la descarga: nunca se sobrescribe una operacion pendiente rechazada.
        for (fila in local.q.pendientes().executeAsList()) {
            val p=fila.dominio()
            try {
                val body=PesajePeticionDto(p.rutaId,p.pesaje.puntoId,p.pesaje.material,p.pesaje.pesoBruto,p.pesaje.tara,p.observacion,p.clave,p.pesaje.humedadEstimada)
                when(p.operacion) {
                    "CREAR" -> {
                        val definitivo=api.crear(body)
                        local.db.transaction { local.q.borrarPesaje(p.pesaje.id); local.definitivo(definitivo,p.clave) }
                    }
                    "EDITAR" -> local.definitivo(api.corregir(p.pesaje.id,body),p.clave)
                    "ANULAR" -> { api.anular(p.pesaje.id,(p.pesaje.estado as EstadoPesaje.Anulado).motivo); local.guardar(p.copy(pesaje=p.pesaje.copy(pendiente=false),operacion="",error=null)) }
                }
                enviados++
            } catch(e: ErrorApi) {
                if(e !is ErrorApi.NoAutorizado) local.q.errorPendiente(e.message,p.pesaje.id)
                if(e is ErrorApi.Validacion) throw ErrorApi.Validacion(e.campos,e.message.orEmpty(),p.pesaje.id,p.pesaje.puntoId)
                throw e
            }
        }
        val recibidos=descargar()
        ResumenSync(enviados,recibidos)
    }
    /** Refresco de lectura: nunca envia ni descarta una operacion pendiente. */
    override suspend fun refrescarDatos(): Int = mutex.withLock { sesion(); descargar() }
    private suspend fun descargar(): Int {
        val padron=api.puntos()
        val dia=reloj.ahoraIso().substringBefore('T')
        val actual=local.q.ruta().executeAsOneOrNull()
        val elegida=seleccion ?: actual?.takeIf { it.fecha==dia }?.id
        val ruta=try { if(elegida==null) api.ruta() else api.ruta(elegida) } catch(e: ErrorApi.NoEncontrado) { seleccion=null; null }
        val fecha=fechaRutas ?: dia
        val rutas=api.rutas(fecha)
        val pesajes=padron.flatMap { api.pesajes(it.id) }
        local.db.transaction {
            val pendientes=local.q.pendientes().executeAsList().map { it.id }.toSet()
            local.q.borrarPuntos(); local.q.borrarRuta(); local.q.borrarLimpios(); local.q.borrarPadron()
            if(ruta!=null) {
                local.q.guardarRuta(ruta.id,ruta.fecha,if(ruta.cerrada) 1 else 0,reloj.ahoraIso())
                local.q.asignarUsuarioRuta(ruta.usuarioId,ruta.id)
                ruta.puntos.forEach { local.q.guardarPunto(it.id,it.razonSocial,it.direccion,it.zona,it.materialHabitual,it.secuencia.toLong(),it.motivoOmision) }
            }
            padron.forEach { local.q.guardarPadron(it.id,it.razonSocial,it.direccion,it.zona,it.materialHabitual) }
            pesajes.filter { it.id !in pendientes }.forEach { local.definitivo(it) }
            local.q.borrarRutasFecha(fecha)
            rutas.forEach { local.q.guardarRutaDisponible(it.id,it.fecha,it.usuarioId,it.usuario,if(it.cerrada) 1 else 0) }
        }
        return pesajes.size
    }
    override suspend fun cargarRutas(fecha: String) = mutex.withLock {
        sesion()
        val rutas=api.rutas(fecha)
        local.db.transaction {
            local.q.borrarRutasFecha(fecha)
            rutas.forEach { local.q.guardarRutaDisponible(it.id,it.fecha,it.usuarioId,it.usuario,if(it.cerrada) 1 else 0) }
        }
        fechaRutas=fecha
    }
    override suspend fun seleccionarRuta(id: Long) = mutex.withLock {
        val s=sesion()
        if(s.privilegios.none { it=="RUTA_CERRAR" || it=="PUNTO_GESTIONAR" }) throw ErrorApi.SinPrivilegio("No puede supervisar rutas ajenas")
        if(local.q.pendientes().executeAsList().isNotEmpty()) throw ErrorApi.Conflicto("Sincroniza o resuelve los pendientes antes de cambiar de ruta")
        val anterior=seleccion
        seleccion=id
        try { descargar() } catch(e: Exception) { seleccion=anterior; throw e }
        Unit
    }
    override suspend fun cerrarRuta() = mutex.withLock {
        privilegio("RUTA_CERRAR")
        if(local.q.pendientes().executeAsList().isNotEmpty()) throw ErrorApi.Conflicto("Sincroniza los pesajes antes de cerrar")
        val r=local.q.ruta().executeAsOneOrNull() ?: throw ErrorApi.NoEncontrado("No hay ruta")
        api.cerrar(r.id)
        local.db.transaction { local.q.cerrarRuta(r.id); local.q.cerrarRutaDisponible(r.id) }
        Unit
    }
    override suspend fun omitir(punto: Long,motivo: String) = mutex.withLock {
        privilegio("PESAJE_CREAR")
        if(motivo.isBlank()) throw ErrorApi.Validacion(mapOf("motivo" to "Indica un motivo"),"Motivo obligatorio")
        val r=local.q.ruta().executeAsOneOrNull() ?: throw ErrorApi.NoEncontrado("No hay ruta")
        api.omitir(r.id,punto,motivo)
    }
    override suspend fun cerrarSesion() = mutex.withLock { seguro.borrar(); local.limpiar(); seleccion=null; fechaRutas=null }
    override suspend fun descartarPendiente(id: Long) = mutex.withLock {
        sesion()
        if(local.q.pendientes().executeAsList().none { it.id==id }) throw ErrorApi.Conflicto("Solo se descartan cambios pendientes")
        local.q.borrarPesaje(id)
        Unit
    }
}
