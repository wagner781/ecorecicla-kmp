package pe.upeu.ecorecicla.data.repository

import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.data.remote.dto.*

class AdministracionRepositoryImpl(private val api: EcoApi) : AdministracionRepository {
    override suspend fun maestros()=MaestrosAdministracion(api.usuarios().map { it.dominio() },api.roles().map { it.dominio() },api.privilegios())
    override suspend fun guardarPunto(id: Long?,form: FormularioPunto) { api.guardarPunto(id,PuntoPeticionDto(form.razonSocial,form.direccion,form.zona,form.materialHabitual)) }
    override suspend fun eliminarPunto(id: Long)=api.eliminarPunto(id)
    override suspend fun guardarUsuario(id: Long?,form: FormularioUsuario) { api.guardarUsuario(id,UsuarioPeticionDto(form.usuario,form.nombre,form.rol,form.password,form.activo)) }
    override suspend fun desactivarUsuario(id: Long)=api.desactivarUsuario(id)
    override suspend fun guardarRol(nombre: String?,form: FormularioRol) { api.guardarRol(nombre,RolPeticionDto(form.nombre,form.privilegios)) }
    override suspend fun eliminarRol(nombre: String)=api.eliminarRol(nombre)
    override suspend fun guardarRuta(id: Long?,form: FormularioRuta) { api.guardarRuta(id,RutaPeticionDto(form.usuarioId,form.fecha,form.puntoIds)) }
}
