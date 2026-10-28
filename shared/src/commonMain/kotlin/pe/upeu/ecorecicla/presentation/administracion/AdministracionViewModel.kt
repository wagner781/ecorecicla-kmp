package pe.upeu.ecorecicla.presentation.administracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.*
import pe.upeu.ecorecicla.presentation.navigation.Pantalla

data class AdminUiState(val usuarios: List<UsuarioGestion> = emptyList(),val roles: List<RolGestion> = emptyList(),val privilegios: List<String> = emptyList(),
    val cargando: Boolean=false,val mensaje: String?=null,val campos: Map<String,String> = emptyMap(),val reintentar: Boolean=false,
    val destino: Pantalla?=null,val fechaDestino: String?=null,val cargado: Boolean=false)

class AdministracionViewModel(private val casos: AdministracionUseCases,private val catalogo: EcoRepository,private val cerrar: CerrarSesionUseCase) : ViewModel() {
    private val mutable=MutableStateFlow(AdminUiState())
    val state=mutable.asStateFlow()
    private var trabajo: Job?=null
    private var ultima: (suspend () -> Unit)?=null
    private var posterior: (suspend () -> Unit)?=null
    fun limpiar() { trabajo?.cancel(); ultima=null; mutable.value=AdminUiState() }
    fun limpiarMensaje() { mutable.update { it.copy(mensaje=null,campos=emptyMap(),reintentar=false) } }
    fun consumirDestino() { mutable.update { it.copy(destino=null,fechaDestino=null) } }
    fun reintentar() { ultima?.let { ejecutar(it) } }
    private suspend fun maestros() { val m=casos.maestros(); mutable.update { it.copy(usuarios=m.usuarios,roles=m.roles,privilegios=m.privilegios,cargado=true) } }
    fun cargar(forzar: Boolean=false) { if(!state.value.cargado || forzar) ejecutar { maestros() } }
    private fun ejecutar(block: suspend () -> Unit) {
        if(state.value.cargando) return
        posterior=null
        mutable.update { it.copy(cargando=true,mensaje=null,campos=emptyMap(),reintentar=false) }
        trabajo=viewModelScope.launch {
            try { block(); ultima=null }
            catch(e: CancellationException) { throw e }
            catch(e: ErrorApi.NoAutorizado) { cerrar(); mutable.value=AdminUiState() }
            catch(e: ErrorApi) {
                if(e is ErrorApi.NoEncontrado) {
                    try { catalogo.refrescarDatos(); maestros() }
                    catch(c: CancellationException) { throw c }
                    catch(c: ErrorApi.NoAutorizado) { cerrar(); mutable.value=AdminUiState(); return@launch }
                    catch(_: Exception) {}
                }
                val recuperable=e is ErrorApi.SinConexion || e is ErrorApi.TiempoAgotado || e is ErrorApi.Servidor
                ultima=if(recuperable) posterior ?: block else null
                mutable.update { it.copy(mensaje=e.message,campos=(e as? ErrorApi.Validacion)?.campos.orEmpty(),reintentar=recuperable) }
            }
            catch(e: Exception) { ultima=posterior ?: block; mutable.update { it.copy(mensaje="No se pudo completar la operacion",reintentar=true) } }
            finally { mutable.update { it.copy(cargando=false) } }
        }
    }
    private suspend fun despues(destino: Pantalla,mensaje: String,accion: suspend () -> Unit) {
        val refresco: suspend () -> Unit={ accion(); mutable.update { it.copy(destino=destino,mensaje=mensaje) } }
        posterior=refresco; refresco()
    }
    fun guardarPunto(id: Long?,form: FormularioPunto)=ejecutar {
        casos.punto(id,form); despues(Pantalla.Padron,"Punto guardado") { catalogo.refrescarDatos() }
    }
    fun eliminarPunto(id: Long)=ejecutar {
        casos.eliminarPunto(id); despues(Pantalla.Padron,"Punto eliminado") { catalogo.refrescarDatos() }
    }
    fun guardarUsuario(id: Long?,form: FormularioUsuario)=ejecutar {
        casos.usuario(id,form); despues(Pantalla.Usuarios,"Usuario guardado; los cambios de acceso requieren iniciar sesion de nuevo") { maestros() }
    }
    fun desactivar(id: Long)=ejecutar {
        casos.desactivar(id); despues(Pantalla.Usuarios,"Usuario desactivado") { maestros() }
    }
    fun guardarRol(nombre: String?,form: FormularioRol)=ejecutar {
        casos.rol(nombre,form); despues(Pantalla.Roles,"Rol guardado; sus usuarios deben iniciar sesion nuevamente") { maestros() }
    }
    fun eliminarRol(nombre: String)=ejecutar {
        casos.eliminarRol(nombre); despues(Pantalla.Roles,"Rol eliminado") { maestros() }
    }
    fun guardarRuta(id: Long?,form: FormularioRuta)=ejecutar {
        casos.ruta(id,form)
        val refresco: suspend () -> Unit={ catalogo.cargarRutas(form.fecha); mutable.update { it.copy(destino=Pantalla.Rutas,fechaDestino=form.fecha,mensaje="Asignacion guardada") } }
        posterior=refresco; refresco()
    }
}
