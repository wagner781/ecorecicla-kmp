package pe.upeu.ecorecicla.di

import org.koin.dsl.module
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.repository.EcoRepositoryOffline
import pe.upeu.ecorecicla.data.repository.SesionRepositoryImpl
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.*
import pe.upeu.ecorecicla.presentation.listado.EcoViewModel

val redModule=module {
    single { val local=get<BaseLocal>(); crearCliente(get(),get(),get()) { local.limpiar() } }
    single { EcoApi(get()) }
}
val dataModule=module {
    single<EcoRepository> { EcoRepositoryOffline(get(),get(),get(),get()) }
    single<SesionRepository> { SesionRepositoryImpl(get(),get(),get()) }
}
val domainModule=module {
    factory { SincronizarUseCase(get()) }; factory { RegistrarPesajeUseCase(get()) }
    factory { IniciarSesionUseCase(get()) }; factory { ConsultarSesionUseCase(get()) }; factory { CerrarSesionUseCase(get()) }
}
val presentationModule=module { factory { EcoViewModel(get(),get(),get(),get(),get(),get(),get()) } }
