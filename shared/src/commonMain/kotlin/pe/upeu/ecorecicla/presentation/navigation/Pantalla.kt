package pe.upeu.ecorecicla.presentation.navigation

sealed interface Pantalla {
    data object Ruta : Pantalla
    data object Perfil : Pantalla
    data object Padron : Pantalla
    data object Rutas : Pantalla
    data object Administracion : Pantalla
    data object Usuarios : Pantalla
    data object Roles : Pantalla
    data class PuntoFormulario(val id: Long?=null) : Pantalla
    data class UsuarioFormulario(val id: Long?=null) : Pantalla
    data class RolFormulario(val nombre: String?=null) : Pantalla
    data class Asignacion(val id: Long?=null) : Pantalla
    data class Detalle(val puntoId: Long) : Pantalla
    data class Formulario(val puntoId: Long,val pesajeId: Long?=null) : Pantalla
}
