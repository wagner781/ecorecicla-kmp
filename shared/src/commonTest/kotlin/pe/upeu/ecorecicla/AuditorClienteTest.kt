package pe.upeu.ecorecicla

import kotlin.test.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.lifecycle.viewModelScope
import io.ktor.client.engine.mock.MockEngine
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.repository.EcoRepositoryOffline
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.*
import pe.upeu.ecorecicla.presentation.listado.EcoViewModel
import pe.upeu.ecorecicla.presentation.navigation.Pantalla

@OptIn(ExperimentalCoroutinesApi::class)
class AuditorClienteTest {
    private val sesion=Sesion("jwt","auditor1","AUDITOR",setOf("PUNTO_LEER","PESAJE_LEER"))
    private val reloj=object : Reloj { override fun ahoraIso()="2026-10-28T08:00:00-05:00"; override fun claveUnica()="x" }
    @Test fun auditorNoPuedeEncolarEscriturasNiCerrarRuta()=runTest {
        val driver=driverMemoria(); val seguro=SeguroMemoria().apply { guardar(this@AuditorClienteTest.sesion,3600) }
        var llamadas=0
        val http=crearCliente(MockEngine { llamadas++; error("No debe llegar a red") },ApiConfig("https://eco.test/api/v1/"),seguro)
        try {
            val repo=EcoRepositoryOffline(BaseLocal(EcoDatabase(driver)),EcoApi(http),seguro,reloj)
            assertFailsWith<ErrorApi.SinPrivilegio> { repo.registrar(FormularioPesaje(1,"Papel",20.0,2.0,null)) }
            assertFailsWith<ErrorApi.SinPrivilegio> { repo.registrar(FormularioPesaje(1,"Papel",20.0,2.0,null),1) }
            assertFailsWith<ErrorApi.SinPrivilegio> { repo.anular(1,"Prueba") }
            assertFailsWith<ErrorApi.SinPrivilegio> { repo.cerrarRuta() }
            assertFailsWith<ErrorApi.SinPrivilegio> { repo.omitir(1,"Prueba") }
            assertEquals(0,llamadas); assertTrue(repo.pesajes().first().isEmpty()); assertNotNull(seguro.leer())
        } finally { http.close(); driver.close() }
    }
    @Test fun consultaSeReconocePorPrivilegiosYAbrePadronSinRuta()=runTest {
        assertTrue(sesion.soloLectura)
        assertTrue(sesion.copy(rol="OTRO_NOMBRE").soloLectura)
        assertFalse(sesion.copy(privilegios=sesion.privilegios+"PUNTO_GESTIONAR").soloLectura)
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val datos=DatosViewModel(); val sesiones=SesionViewModel().apply { this.sesion.value=this@AuditorClienteTest.sesion }
        val vm=EcoViewModel(datos,IniciarSesionUseCase(sesiones),ConsultarSesionUseCase(sesiones),CerrarSesionUseCase(sesiones),SincronizarUseCase(datos),RegistrarPesajeUseCase(datos),MutableStateFlow(false),CatalogoUseCases(datos,reloj))
        try { runCurrent(); assertEquals(Pantalla.Padron,vm.state.value.pantalla) }
        finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
}
