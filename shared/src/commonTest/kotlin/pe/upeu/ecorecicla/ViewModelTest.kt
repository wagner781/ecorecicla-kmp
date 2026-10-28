package pe.upeu.ecorecicla

import kotlin.test.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.*
import pe.upeu.ecorecicla.presentation.listado.EcoViewModel
import pe.upeu.ecorecicla.presentation.navigation.Pantalla

class DatosViewModel : EcoRepository {
    val puntos=MutableStateFlow(emptyList<PuntoAcopio>()); val registros=MutableStateFlow(emptyList<Registro>())
    val actual=MutableStateFlow<RutaLocal?>(RutaLocal(1,"2026-10-28",false,""))
    var cierres=0; var envios=0; var guardados=0; var refrescos=0
    var falloCierre: ErrorApi?=null; var falloEnvio: ErrorApi?=null
    override fun puntos()=puntos
    override fun pendientes()=registros.map { lista -> lista.count { it.pesaje.pendiente }.toLong() }
    override fun pesajes()=registros
    override fun ruta()=actual
    override fun padron()=puntos
    override fun rutas()=flowOf(emptyList<RutaResumen>())
    override suspend fun refrescarDatos(): Int { refrescos++; return 0 }
    override suspend fun cargarRutas(fecha: String) {}
    override suspend fun seleccionarRuta(id: Long) {}
    override suspend fun registrar(formulario: FormularioPesaje,id: Long?) { guardados++ }
    override suspend fun anular(id: Long,motivo: String) {}
    override suspend fun sincronizar(): ResumenSync { envios++; falloEnvio?.let { throw it }; return ResumenSync(1,0) }
    override suspend fun cerrarRuta() { cierres++; falloCierre?.let { throw it }; actual.value=actual.value?.copy(cerrada=true) }
    override suspend fun omitir(punto: Long,motivo: String) {}
    override suspend fun descartarPendiente(id: Long) {}
    override suspend fun cerrarSesion() {}
}
class SesionViewModel : SesionRepository {
    val sesion=MutableStateFlow<Sesion?>(Sesion("jwt","supervisor1","SUPERVISOR",setOf("RUTA_CERRAR","PESAJE_CREAR")))
    override fun observar()=sesion
    override fun actual()=sesion.value
    override suspend fun iniciar(usuario: String,password: String)=sesion.value!!
    override suspend fun perfil()=sesion.value!!
    override suspend fun cerrar() { sesion.value=null }
}
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private fun crear(datos: DatosViewModel,sesion: SesionViewModel,conexion: MutableStateFlow<Boolean>): EcoViewModel {
        val reloj=object : Reloj { override fun ahoraIso()="2026-10-28T08:00:00-05:00"; override fun claveUnica()="prueba" }
        return EcoViewModel(datos,IniciarSesionUseCase(sesion),ConsultarSesionUseCase(sesion),CerrarSesionUseCase(sesion),SincronizarUseCase(datos),RegistrarPesajeUseCase(datos),conexion,CatalogoUseCases(datos,reloj))
    }
    @Test fun reintentarCierreRepiteCierreYSuErrorNoSeConvierteEnSincronizar()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val datos=DatosViewModel(); val vm=crear(datos,SesionViewModel(),MutableStateFlow(false))
        try {
            runCurrent(); datos.falloCierre=ErrorApi.Servidor(); vm.cerrarRuta(); runCurrent()
            assertTrue(vm.state.value.reintentar)
            datos.falloCierre=null; vm.reintentar(); runCurrent()
            assertEquals(2,datos.cierres); assertEquals(0,datos.envios); assertFalse(vm.state.value.reintentar)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
    @Test fun reintentarEnvioNoVuelveAGuardarOtroPesajeLocal()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val datos=DatosViewModel(); val conexion=MutableStateFlow(false); val vm=crear(datos,SesionViewModel(),conexion)
        try {
            runCurrent(); conexion.value=true; runCurrent(); datos.envios=0
            datos.falloEnvio=ErrorApi.SinConexion()
            vm.guardar(FormularioPesaje(1,"Papel",20.0,2.0,null),null); runCurrent()
            assertEquals(1,datos.guardados); assertTrue(vm.state.value.reintentar)
            datos.falloEnvio=null; vm.reintentar(); runCurrent()
            assertEquals(1,datos.guardados); assertEquals(2,datos.envios)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
    @Test fun un404RefrescaElListadoSinOfrecerReintentarNiCerrarSesion()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val datos=DatosViewModel(); val vm=crear(datos,SesionViewModel(),MutableStateFlow(false))
        try {
            runCurrent(); datos.falloCierre=ErrorApi.NoEncontrado("Ruta inexistente"); vm.cerrarRuta(); runCurrent()
            assertEquals(1,datos.refrescos); assertFalse(vm.state.value.reintentar); assertNotNull(vm.state.value.sesion)
            assertEquals("Ruta inexistente",vm.state.value.mensaje)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
    @Test fun revocacionEnOtraPantallaLlevaAlLoginPorElFlujoDeSesion()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val sesiones=SesionViewModel(); val vm=crear(DatosViewModel(),sesiones,MutableStateFlow(false))
        try { runCurrent(); sesiones.cerrar(); runCurrent(); assertNull(vm.state.value.sesion) }
        finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
}
