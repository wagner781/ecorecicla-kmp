package pe.upeu.ecorecicla.di

import org.koin.dsl.module
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.repository.EcoRepositoryOffline
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.*
import pe.upeu.ecorecicla.presentation.listado.EcoViewModel

val redModule=module {
    single { val local=get<BaseLocal>(); crearCliente(get(),get(),get()) { local.limpiar() } }
    single { EcoApi(get()) }
}
val dataModule=module { single<EcoRepository> { EcoRepositoryOffline(get(),get(),get(),get()) } }
val domainModule=module { factory { SincronizarUseCase(get()) }; factory { RegistrarPesajeUseCase(get()) } }
val presentationModule=module { factory { EcoViewModel(get(),get(),get(),get(),get(),get()) } }
