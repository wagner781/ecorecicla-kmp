package pe.upeu.ecorecicla.data.remote.dto

import kotlinx.serialization.Serializable
import pe.upeu.ecorecicla.domain.model.*

@Serializable data class UsuarioGestionDto(val id: Long,val usuario: String,val nombre: String,val rol: String,val privilegios: Set<String>,val activo: Boolean=true) {
    fun dominio()=UsuarioGestion(id,usuario,nombre,rol,privilegios,activo)
}
@Serializable data class RolGestionDto(val nombre: String,val privilegios: Set<String>) { fun dominio()=RolGestion(nombre,privilegios) }
@Serializable data class PuntoPeticionDto(val razonSocial: String,val direccion: String,val zona: String,val materialHabitual: String)
@Serializable data class UsuarioPeticionDto(val usuario: String,val nombre: String,val rol: String,val password: String?,val activo: Boolean)
@Serializable data class RolPeticionDto(val nombre: String,val privilegios: Set<String>)
@Serializable data class RutaPeticionDto(val usuarioId: Long,val fecha: String,val puntoIds: List<Long>)
