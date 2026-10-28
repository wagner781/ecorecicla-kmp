package pe.upeu.ecorecicla.presentation.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.*
import pe.upeu.ecorecicla.presentation.navigation.Pantalla

data class UiState(val sesion: Sesion?=null,val puntos: List<PuntoAcopio> = emptyList(),val pesajes: List<Registro> = emptyList(),val ruta: RutaLocal?=null,val pantalla: Pantalla=Pantalla.Ruta,val cargando: Boolean=false,val mensaje: String?=null,val campos: Map<String,String> = emptyMap(),val reintentar: Boolean=false,val conectado: Boolean=false)

class EcoViewModel(private val repo: EcoRepository,private val api: EcoApi,private val seguro: AlmacenSeguro,private val sync: SincronizarUseCase,private val registrar: RegistrarPesajeUseCase,conectividad: Flow<Boolean>) : ViewModel() {
    private val mutable=MutableStateFlow(UiState(sesion=seguro.leer()))
    val state: StateFlow<UiState> = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            combine(repo.puntos(),repo.pesajes(),repo.ruta()) { p,w,r -> Triple(p,w,r) }.collect { (p,w,r) -> mutable.update { it.copy(puntos=p,pesajes=w,ruta=r) } }
        }
        viewModelScope.launch {
            conectividad.distinctUntilChanged().collect { online ->
                mutable.update { it.copy(conectado=online) }
                if(online && seguro.leer()!=null) sincronizar()
            }
        }
        viewModelScope.launch {
            while(isActive) { delay(30000); verificarSesion() }
        }
    }
    fun verificarSesion() {
        if(state.value.sesion!=null && seguro.leer()==null) ejecutar { repo.cerrarSesion(); mutable.update { UiState(mensaje="Tu sesion vencio",conectado=it.conectado) } }
    }
    private fun ejecutar(block: suspend () -> Unit) {
        if(state.value.cargando) return
        mutable.update { it.copy(cargando=true,mensaje=null,campos=emptyMap(),reintentar=false) }
        viewModelScope.launch {
            try { block() }
            catch(e: CancellationException) { throw e }
            catch(e: ErrorApi) {
                if(e is ErrorApi.NoAutorizado) {
                    repo.cerrarSesion()
                    mutable.update { UiState(mensaje=e.message,conectado=it.conectado) }
                } else mutable.update { it.copy(mensaje=e.message,campos=(e as? ErrorApi.Validacion)?.campos.orEmpty(),reintentar=e is ErrorApi.Servidor || e is ErrorApi.SinConexion || e is ErrorApi.TiempoAgotado) }
            }
            catch(e: Exception) { mutable.update { it.copy(mensaje="No se pudo completar la operacion",reintentar=true) } }
            finally { mutable.update { it.copy(cargando=false) } }
        }
    }
    fun login(usuario: String,password: String)=ejecutar {
        val sesion=api.login(usuario,password)
        repo.cerrarSesion()
        seguro.guardar(sesion.dominio(),sesion.expiresIn)
        mutable.update { it.copy(sesion=sesion.dominio(),pantalla=Pantalla.Ruta) }
        val resultado=sync(); mutable.update { it.copy(mensaje="${resultado.recibidos} pesajes recibidos") }
    }
    fun navegar(pantalla: Pantalla) { mutable.update { it.copy(pantalla=pantalla,mensaje=null,campos=emptyMap(),reintentar=false) } }
    fun sincronizar()=ejecutar {
        val resultado=sync()
        mutable.update { it.copy(mensaje="Sincronizado: ${resultado.enviados} enviados, ${resultado.recibidos} recibidos") }
    }
    fun guardar(form: FormularioPesaje,id: Long?)=ejecutar {
        registrar(form,id)
        mutable.update { it.copy(pantalla=Pantalla.Detalle(form.puntoId),mensaje="Guardado en el dispositivo; pendiente de envio") }
        if(state.value.conectado) {
            val resultado=sync(); mutable.update { it.copy(mensaje="${resultado.enviados} enviados, ${resultado.recibidos} recibidos") }
        }
    }
    fun anular(id: Long,motivo: String)=ejecutar {
        repo.anular(id,motivo); mutable.update { it.copy(mensaje="Anulacion pendiente de sincronizar") }
        if(state.value.conectado) sync()
    }
    fun omitir(punto: Long,motivo: String)=ejecutar { repo.omitir(punto,motivo); sync(); mutable.update { it.copy(mensaje="Omision guardada") } }
    fun cerrarRuta()=ejecutar { repo.cerrarRuta(); mutable.update { it.copy(mensaje="Ruta cerrada") } }
    fun perfil()=ejecutar {
        mutable.update { it.copy(pantalla=Pantalla.Perfil) }
        if(state.value.conectado) {
            val user=api.me(); val session=seguro.leer() ?: throw ErrorApi.NoAutorizado()
            mutable.update { it.copy(sesion=session.copy(usuario=user.usuario,rol=user.rol,privilegios=user.privilegios)) }
        }
    }
    fun salir()=ejecutar { repo.cerrarSesion(); mutable.update { UiState(conectado=it.conectado) } }
}
