package pe.upeu.ecorecicla.domain.model

data class RutaResumen(val id: Long,val fecha: String,val usuarioId: Long,val usuario: String,val cerrada: Boolean)
data class UsuarioGestion(val id: Long,val usuario: String,val nombre: String,val rol: String,val privilegios: Set<String>,val activo: Boolean)
data class RolGestion(val nombre: String,val privilegios: Set<String>)
data class FormularioPunto(val razonSocial: String,val direccion: String,val zona: String,val materialHabitual: String)
data class FormularioUsuario(val usuario: String,val nombre: String,val rol: String,val password: String?,val activo: Boolean=true)
data class FormularioRol(val nombre: String,val privilegios: Set<String>)
data class FormularioRuta(val usuarioId: Long,val fecha: String,val puntoIds: List<Long>)
