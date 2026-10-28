package pe.upeu.ecorecicla

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import androidx.lifecycle.viewModelScope
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.*
import pe.upeu.ecorecicla.presentation.listado.EcoViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class FiltroRutaTest {
    private val filtrar=FiltrarRutaUseCase()
    private val puntos=listOf(
        PuntoAcopio(1,"Papel Uno","Av. 1","Norte","Papel",1,false),
        PuntoAcopio(2,"Papel Dos","Av. 2","Sur","Papel",2,true),
        PuntoAcopio(3,"Papel Tres","Av. 3","Sur","Papel",3,false,"Cerrado"),
        PuntoAcopio(4,"Metal Uno","Av. 4","Centro","Metal",4,false))
    @Test fun combinaBusquedaYPendientesSinPerderOrden() {
        assertEquals(listOf(1L),filtrar(puntos,"  PAPEL  ",true).map { it.id })
        assertEquals(listOf(1L,2L,3L),filtrar(puntos,"papel",false).map { it.id })
        assertEquals(listOf(1L,4L),filtrar(puntos,"",true).map { it.id })
        assertEquals(4L,filtrar(puntos,"Av. 4",false).single().id)
        assertTrue(filtrar(puntos,"no existe",true).isEmpty())
    }
    @Test fun filtroActivoSeActualizaAlCambiarElCacheSinConexion()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val datos=DatosViewModel(); val sesion=SesionViewModel()
        val reloj=object : Reloj { override fun ahoraIso()="2026-10-28T08:00:00-05:00"; override fun claveUnica()="x" }
        val vm=EcoViewModel(datos,IniciarSesionUseCase(sesion),ConsultarSesionUseCase(sesion),CerrarSesionUseCase(sesion),SincronizarUseCase(datos),RegistrarPesajeUseCase(datos),MutableStateFlow(false),CatalogoUseCases(datos,reloj))
        try {
            datos.puntos.value=puntos; runCurrent(); vm.buscarRuta("papel"); vm.mostrarSoloPendientes(true)
            assertEquals(listOf(1L),vm.state.value.puntosVisibles.map { it.id })
            datos.puntos.value=puntos.map { if(it.id==1L) it.copy(atendido=true) else it }; runCurrent()
            assertTrue(vm.state.value.puntosVisibles.isEmpty()); assertEquals(0,datos.envios)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
}
