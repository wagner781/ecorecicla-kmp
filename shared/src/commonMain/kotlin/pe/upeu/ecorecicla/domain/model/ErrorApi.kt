package pe.upeu.ecorecicla.domain.model

sealed class ErrorApi(message: String) : Exception(message) {
    class Validacion(val campos: Map<String,String>, message: String, val pesajeId: Long?=null, val puntoId: Long?=null) : ErrorApi(message)
    class NoAutorizado : ErrorApi("La sesion vencio. Inicia sesion nuevamente")
    class SinPrivilegio(message: String) : ErrorApi(message)
    class NoEncontrado(message: String) : ErrorApi(message)
    class Conflicto(message: String) : ErrorApi(message)
    class Servidor : ErrorApi("El servidor no pudo responder. Puedes reintentar")
    class SinConexion : ErrorApi("Sin conexion. Los cambios locales siguen guardados")
    class TiempoAgotado : ErrorApi("Se agoto el tiempo de espera. Puedes reintentar")
}
