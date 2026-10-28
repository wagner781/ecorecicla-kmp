package pe.upeu.ecorecicla.presentation.navigation

sealed interface Pantalla {
    data object Ruta : Pantalla
    data object Perfil : Pantalla
    data object Padron : Pantalla
    data object Rutas : Pantalla
    data class Detalle(val puntoId: Long) : Pantalla
    data class Formulario(val puntoId: Long,val pesajeId: Long?=null) : Pantalla
}
