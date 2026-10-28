package pe.upeu.ecorecicla.domain.usecase

import pe.upeu.ecorecicla.domain.repository.*

class SincronizarUseCase(private val repository: EcoRepository) { suspend operator fun invoke()=repository.sincronizar() }
class RegistrarPesajeUseCase(private val repository: EcoRepository) { suspend operator fun invoke(formulario: FormularioPesaje,id: Long?=null)=repository.registrar(formulario,id) }
