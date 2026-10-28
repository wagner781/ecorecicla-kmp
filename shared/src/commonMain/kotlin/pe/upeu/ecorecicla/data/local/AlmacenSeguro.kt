package pe.upeu.ecorecicla.data.local

import pe.upeu.ecorecicla.domain.model.Sesion

interface AlmacenSeguro {
    fun leer(): Sesion?
    fun guardar(sesion: Sesion, expiresIn: Long)
    fun borrar()
}
