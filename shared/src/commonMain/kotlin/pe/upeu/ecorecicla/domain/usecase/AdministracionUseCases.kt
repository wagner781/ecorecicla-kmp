package pe.upeu.ecorecicla.domain.usecase

import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*

class AdministracionUseCases(private val admin: AdministracionRepository,private val sesiones: SesionRepository) {
    private fun autorizar() {
        val s=sesiones.actual() ?: throw ErrorApi.NoAutorizado()
        if("PUNTO_GESTIONAR" !in s.privilegios) throw ErrorApi.SinPrivilegio("No posee permiso de administracion")
    }
    suspend fun maestros(): MaestrosAdministracion { autorizar(); return admin.maestros() }
    suspend fun punto(id: Long?,b: FormularioPunto) {
        autorizar()
        val campos=mapOf("razonSocial" to b.razonSocial,"direccion" to b.direccion,"zona" to b.zona,"materialHabitual" to b.materialHabitual).filterValues { it.isBlank() }.mapValues { "Campo obligatorio" }
        if(campos.isNotEmpty()) throw ErrorApi.Validacion(campos,"Revisa los campos")
        admin.guardarPunto(id,b)
    }
    suspend fun eliminarPunto(id: Long) { autorizar(); admin.eliminarPunto(id) }
    suspend fun usuario(id: Long?,b: FormularioUsuario) {
        autorizar()
        val campos=buildMap {
            if(!Regex("[a-zA-Z0-9._-]{3,80}").matches(b.usuario)) put("usuario","Usa de 3 a 80 letras, numeros, punto, guion o guion bajo")
            if(b.nombre.isBlank()) put("nombre","Nombre obligatorio")
            if(b.rol.isBlank()) put("rol","Selecciona un rol")
            if((id==null && b.password.isNullOrEmpty()) || (b.password!=null && (b.password.length<8 || b.password.encodeToByteArray().size>72))) put("password","Minimo 8 caracteres y maximo 72 bytes UTF-8")
        }
        if(campos.isNotEmpty()) throw ErrorApi.Validacion(campos,"Revisa los campos")
        admin.guardarUsuario(id,b)
    }
    suspend fun desactivar(id: Long) { autorizar(); admin.desactivarUsuario(id) }
    suspend fun rol(nombre: String?,b: FormularioRol) {
        autorizar()
        if(!Regex("[A-Z][A-Z0-9_]{2,39}").matches(b.nombre)) throw ErrorApi.Validacion(mapOf("nombre" to "Usa de 3 a 40 mayusculas, numeros o guion bajo"),"Identificador de rol invalido")
        admin.guardarRol(nombre,b)
    }
    suspend fun eliminarRol(nombre: String) { autorizar(); admin.eliminarRol(nombre) }
    suspend fun ruta(id: Long?,b: FormularioRuta) {
        autorizar()
        val campos=buildMap {
            if(b.usuarioId<=0) put("usuarioId","Selecciona un usuario")
            if(!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(b.fecha)) put("fecha","Usa AAAA-MM-DD")
            if(b.puntoIds.isEmpty()) put("puntoIds","Selecciona al menos un punto")
        }
        if(campos.isNotEmpty()) throw ErrorApi.Validacion(campos,"Revisa los campos")
        admin.guardarRuta(id,b)
    }
}
