package pe.upeu.ecorecicla.domain.usecase

import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.model.*

class CatalogoUseCases(private val repo: EcoRepository,private val reloj: Reloj) {
    fun hoy()=reloj.ahoraIso().substringBefore('T')
    suspend fun refrescar()=repo.refrescarDatos()
    suspend fun seleccionar(id: Long)=repo.seleccionarRuta(id)
    suspend fun cargar(fecha: String) {
        if(!Regex("[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])").matches(fecha)) throw ErrorApi.Validacion(mapOf("fecha" to "Usa el formato AAAA-MM-DD"),"Fecha invalida")
        repo.cargarRutas(fecha)
    }
    fun filtrar(puntos: List<PuntoAcopio>,zona: String)=puntos.filter { zona.isBlank() || it.zona.contains(zona.trim(),ignoreCase=true) }
    fun porFecha(rutas: List<RutaResumen>,fecha: String)=rutas.filter { it.fecha==fecha }
}
