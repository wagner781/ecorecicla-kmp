package pe.upeu.ecorecicla.domain.repository

import pe.upeu.ecorecicla.domain.model.*

data class MaestrosAdministracion(val usuarios: List<UsuarioGestion>,val roles: List<RolGestion>,val privilegios: List<String>)
interface AdministracionRepository {
    suspend fun maestros(): MaestrosAdministracion
    suspend fun guardarPunto(id: Long?,form: FormularioPunto)
    suspend fun eliminarPunto(id: Long)
    suspend fun guardarUsuario(id: Long?,form: FormularioUsuario)
    suspend fun desactivarUsuario(id: Long)
    suspend fun guardarRol(nombre: String?,form: FormularioRol)
    suspend fun eliminarRol(nombre: String)
    suspend fun guardarRuta(id: Long?,form: FormularioRuta)
}
