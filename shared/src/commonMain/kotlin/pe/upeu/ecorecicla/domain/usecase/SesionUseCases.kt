package pe.upeu.ecorecicla.domain.usecase

import pe.upeu.ecorecicla.domain.repository.SesionRepository
import pe.upeu.ecorecicla.domain.model.ErrorApi

class IniciarSesionUseCase(private val repo: SesionRepository) {
    suspend operator fun invoke(usuario: String,password: String)=repo.iniciar(usuario.trim(),password)
}
class ConsultarSesionUseCase(private val repo: SesionRepository) {
    fun actual()=repo.actual()
    suspend operator fun invoke()=repo.perfil()
}
class CerrarSesionUseCase(private val repo: SesionRepository) { suspend operator fun invoke()=repo.cerrar() }
