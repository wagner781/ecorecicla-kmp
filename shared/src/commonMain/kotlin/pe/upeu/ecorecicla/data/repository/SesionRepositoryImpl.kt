package pe.upeu.ecorecicla.data.repository

import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.remote.EcoApi

class SesionRepositoryImpl(private val api: EcoApi,private val seguro: AlmacenSeguro,private val datos: EcoRepository) : SesionRepository {
    private val cambios=kotlinx.coroutines.flow.MutableStateFlow(seguro.leer())
    override fun observar(): kotlinx.coroutines.flow.Flow<Sesion?> = cambios
    override fun actual()=seguro.leer()
    override suspend fun iniciar(usuario: String,password: String): Sesion {
        val respuesta=api.login(usuario,password)
        datos.cerrarSesion()
        return respuesta.dominio().also { seguro.guardar(it,respuesta.expiresIn); cambios.value=it }
    }
    override suspend fun perfil(): Sesion {
        val perfil=api.me()
        val sesion=actual() ?: throw ErrorApi.NoAutorizado()
        // No se vuelve a guardar el token: consultar perfil no prolonga la hora de vigencia.
        return sesion.copy(usuario=perfil.usuario,rol=perfil.rol,privilegios=perfil.privilegios,id=perfil.id,nombre=perfil.nombre)
    }
    override suspend fun cerrar() { datos.cerrarSesion(); cambios.value=null }
}
