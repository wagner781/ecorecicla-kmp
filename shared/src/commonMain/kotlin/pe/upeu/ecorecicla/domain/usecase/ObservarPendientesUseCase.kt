package pe.upeu.ecorecicla.domain.usecase

import pe.upeu.ecorecicla.domain.repository.EcoRepository

class ObservarPendientesUseCase(private val repo: EcoRepository) {
    operator fun invoke()=repo.pendientes()
}
