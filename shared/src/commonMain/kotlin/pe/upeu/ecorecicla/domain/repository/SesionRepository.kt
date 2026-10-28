package pe.upeu.ecorecicla.domain.repository

import pe.upeu.ecorecicla.domain.model.Sesion

interface SesionRepository {
    fun observar(): kotlinx.coroutines.flow.Flow<Sesion?>
    fun actual(): Sesion?
    suspend fun iniciar(usuario: String,password: String): Sesion
    suspend fun perfil(): Sesion
    suspend fun cerrar()
}
