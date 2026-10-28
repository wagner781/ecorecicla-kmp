package pe.upeu.ecorecicla.domain.usecase

import pe.upeu.ecorecicla.domain.model.PuntoAcopio

/** Una omision justificada resuelve la visita; no es un pesaje pendiente. */
class FiltrarRutaUseCase {
    operator fun invoke(puntos: List<PuntoAcopio>,busqueda: String,soloPendientes: Boolean): List<PuntoAcopio> {
        val texto=busqueda.trim()
        return puntos.filter { p ->
            (!soloPendientes || (!p.atendido && p.motivoOmision.isNullOrBlank())) &&
                (texto.isEmpty() || listOf(p.razonSocial,p.direccion,p.zona,p.materialHabitual).any { it.contains(texto,ignoreCase=true) })
        }
    }
}
