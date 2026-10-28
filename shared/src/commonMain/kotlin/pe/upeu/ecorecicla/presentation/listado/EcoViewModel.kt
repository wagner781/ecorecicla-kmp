package pe.upeu.ecorecicla.presentation.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.*
import pe.upeu.ecorecicla.presentation.navigation.Pantalla

data class UiState(
    val sesion: Sesion?=null,val puntos: List<PuntoAcopio> = emptyList(),val pesajes: List<Registro> = emptyList(),
    val ruta: RutaLocal?=null,val pantalla: Pantalla=Pantalla.Ruta,val cargando: Boolean=false,
    val mensaje: String?=null,val campos: Map<String,String> = emptyMap(),val reintentar: Boolean=false,val conectado: Boolean=false,
    val padron: List<PuntoAcopio> = emptyList(),val padronVisible: List<PuntoAcopio> = emptyList(),val zona: String="",
    val rutas: List<RutaResumen> = emptyList(),val rutasVisibles: List<RutaResumen> = emptyList(),val fechaRutas: String=""
)

class EcoViewModel(
    private val repo: EcoRepository,private val iniciar: IniciarSesionUseCase,private val consultar: ConsultarSesionUseCase,
    private val cerrar: CerrarSesionUseCase,private val sync: SincronizarUseCase,private val registrar: RegistrarPesajeUseCase,
    conectividad: Flow<Boolean>,private val catalogo: CatalogoUseCases
) : ViewModel() {
    private fun inicio(sesion: Sesion?)=if(sesion!=null && "PESAJE_CREAR" !in sesion.privilegios) Pantalla.Padron else Pantalla.Ruta
    private val mutable=MutableStateFlow(UiState(sesion=consultar.actual(),pantalla=inicio(consultar.actual()),fechaRutas=catalogo.hoy()))
    val state: StateFlow<UiState> = mutable.asStateFlow()
    private var ultimaOperacion: (suspend () -> Unit)?=null
    private var reintentoAlternativo: (suspend () -> Unit)?=null
    init {
        viewModelScope.launch { consultar.observar().collect { s ->
            if(s==null && state.value.sesion!=null) mutable.update { UiState(conectado=it.conectado,fechaRutas=catalogo.hoy()) }
        } }
        viewModelScope.launch { combine(repo.puntos(),repo.pesajes(),repo.ruta()) { p,w,r -> Triple(p,w,r) }.collect { (p,w,r) -> mutable.update { it.copy(puntos=p,pesajes=w,ruta=r) } } }
        viewModelScope.launch { repo.padron().collect { p -> mutable.update { it.copy(padron=p,padronVisible=catalogo.filtrar(p,it.zona)) } } }
        viewModelScope.launch { repo.rutas().collect { r -> mutable.update { it.copy(rutas=r,rutasVisibles=catalogo.porFecha(r,it.fechaRutas)) } } }
        viewModelScope.launch {
            conectividad.distinctUntilChanged().collect { online ->
                mutable.update { it.copy(conectado=online) }
                if(online && consultar.actual()!=null) sincronizar()
            }
        }
        viewModelScope.launch { while(isActive) { delay(30000); verificarSesion() } }
    }
    fun verificarSesion() {
        if(state.value.sesion!=null && consultar.actual()==null) ejecutar { finalizar("Tu sesion vencio") }
    }
    private suspend fun finalizar(mensaje: String?=null) {
        cerrar(); ultimaOperacion=null
        mutable.update { UiState(mensaje=mensaje,conectado=it.conectado,fechaRutas=catalogo.hoy()) }
    }
    private fun ejecutar(block: suspend () -> Unit) {
        if(state.value.cargando) return
        reintentoAlternativo=null
        mutable.update { it.copy(cargando=true,mensaje=null,campos=emptyMap(),reintentar=false) }
        viewModelScope.launch {
            try { block(); ultimaOperacion=null }
            catch(e: CancellationException) { throw e }
            catch(e: ErrorApi) {
                if(e is ErrorApi.NoAutorizado) finalizar(e.message)
                else {
                    if(e is ErrorApi.NoEncontrado) {
                        try { catalogo.refrescar() }
                        catch(c: CancellationException) { throw c }
                        catch(c: ErrorApi.NoAutorizado) { finalizar(c.message); return@launch }
                        catch(_: Exception) { /* Conserva el 404 y los pendientes si el refresco pierde conexion. */ }
                    }
                    val recuperable=e is ErrorApi.Servidor || e is ErrorApi.SinConexion || e is ErrorApi.TiempoAgotado
                    ultimaOperacion=if(recuperable) reintentoAlternativo ?: block else null
                    mutable.update {
                        val v=e as? ErrorApi.Validacion
                        val pantalla=if(v?.puntoId!=null && v.pesajeId!=null) Pantalla.Formulario(v.puntoId,v.pesajeId) else it.pantalla
                        it.copy(pantalla=pantalla,mensaje=e.message,campos=v?.campos.orEmpty(),reintentar=recuperable)
                    }
                }
            }
            catch(e: Exception) { ultimaOperacion=reintentoAlternativo ?: block; mutable.update { it.copy(mensaje="No se pudo completar la operacion",reintentar=true) } }
            finally { mutable.update { it.copy(cargando=false) } }
        }
    }
    private suspend fun enviarPendientes() {
        val accion: suspend () -> Unit={ val r=sync(); mutable.update { it.copy(mensaje="Sincronizado: ${r.enviados} enviados, ${r.recibidos} recibidos") } }
        reintentoAlternativo=accion
        accion()
    }
    fun login(usuario: String,password: String)=ejecutar {
        val sesion=iniciar(usuario,password)
        mutable.update { it.copy(sesion=sesion,pantalla=inicio(sesion)) }
        enviarPendientes()
    }
    fun reintentar() { ultimaOperacion?.let { ejecutar(it) } }
    fun navegar(pantalla: Pantalla) { mutable.update { it.copy(pantalla=pantalla,mensaje=null,campos=emptyMap(),reintentar=false) } }
    fun sincronizar()=ejecutar { enviarPendientes() }
    fun guardar(form: FormularioPesaje,id: Long?)=ejecutar {
        registrar(form,id)
        mutable.update { it.copy(pantalla=Pantalla.Detalle(form.puntoId),mensaje="Guardado en el dispositivo; pendiente de envio") }
        if(state.value.conectado) enviarPendientes()
    }
    fun anular(id: Long,motivo: String)=ejecutar {
        repo.anular(id,motivo); mutable.update { it.copy(mensaje="Anulacion pendiente de sincronizar") }
        if(state.value.conectado) enviarPendientes()
    }
    fun omitir(punto: Long,motivo: String)=ejecutar {
        repo.omitir(punto,motivo)
        reintentoAlternativo={ catalogo.refrescar() }
        catalogo.refrescar(); mutable.update { it.copy(mensaje="Omision guardada") }
    }
    fun cerrarRuta()=ejecutar { repo.cerrarRuta(); mutable.update { it.copy(mensaje="Ruta cerrada") } }
    fun descartar(id: Long)=ejecutar {
        repo.descartarPendiente(id)
        if(state.value.conectado) enviarPendientes()
        mutable.update { it.copy(mensaje="Cambio local descartado. Los datos del servidor se conservan") }
    }
    fun filtrarZona(zona: String) { mutable.update { it.copy(zona=zona,padronVisible=catalogo.filtrar(it.padron,zona)) } }
    fun cargarRutas(fecha: String)=ejecutar {
        catalogo.cargar(fecha)
        mutable.update { it.copy(fechaRutas=fecha,rutasVisibles=catalogo.porFecha(it.rutas,fecha)) }
    }
    fun seleccionarRuta(id: Long)=ejecutar { catalogo.seleccionar(id); mutable.update { it.copy(pantalla=Pantalla.Ruta,mensaje="Ruta seleccionada") } }
    fun perfil()=ejecutar {
        mutable.update { it.copy(pantalla=Pantalla.Perfil) }
        if(state.value.conectado) { val session=consultar(); mutable.update { it.copy(sesion=session) } }
    }
    fun salir()=ejecutar { finalizar() }
}
