package pe.upeu.ecorecicla.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.presentation.administracion.*
import pe.upeu.ecorecicla.presentation.listado.*
import pe.upeu.ecorecicla.presentation.navigation.Pantalla

@Composable internal fun UsuariosScreen(s: UiState,a: AdminUiState,vm: EcoViewModel,admin: AdministracionViewModel) {
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("Usuarios",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.UsuarioFormulario())},enabled=!s.cargando && s.conectado) { Text("Nuevo usuario") }
                OutlinedButton(onClick={admin.cargar(true)},enabled=!s.cargando && s.conectado) { Text("Actualizar") }
            }
        }
        if(a.usuarios.isEmpty() && !s.cargando) item { Text("No hay usuarios cargados. Consulta con conexion.") }
        items(a.usuarios,key={it.id}) { u ->
            Card(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.UsuarioFormulario(u.id))},modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(u.nombre,fontWeight=FontWeight.Bold); Text("${u.usuario} · ${u.rol}")
                    Text(if(u.activo) "Activo" else "Inactivo")
                }
            }
        }
    }
}

@Composable internal fun UsuarioFormularioScreen(s: UiState,a: AdminUiState,id: Long?,vm: EcoViewModel,admin: AdministracionViewModel) {
    val previo=a.usuarios.find { it.id==id }
    var usuario by rememberSaveable(id) { mutableStateOf(previo?.usuario.orEmpty()) }
    var nombre by rememberSaveable(id) { mutableStateOf(previo?.nombre.orEmpty()) }
    var password by rememberSaveable(id) { mutableStateOf("") }
    var rol by rememberSaveable(id) { mutableStateOf(previo?.rol.orEmpty()) }
    var activo by rememberSaveable(id) { mutableStateOf(previo?.activo ?: true) }
    var desactivar by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.Usuarios)},enabled=!s.cargando) { Text("Volver a usuarios") }
        Text(if(id==null) "Nuevo usuario" else "Editar usuario",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Campo("Usuario",usuario,{usuario=it},a.campos["usuario"])
        Campo("Nombre",nombre,{nombre=it},a.campos["nombre"])
        OutlinedTextField(password,{password=it},label={Text(if(id==null) "Contrasena" else "Nueva contrasena (opcional)")},visualTransformation=PasswordVisualTransformation(),singleLine=true,isError=a.campos["password"]!=null,supportingText={a.campos["password"]?.let { Text(it) }},modifier=Modifier.fillMaxWidth())
        Text("Rol",fontWeight=FontWeight.Bold)
        a.roles.forEach { r ->
            Row(Modifier.fillMaxWidth().clickable(enabled=!s.cargando) { rol=r.nombre },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=rol==r.nombre,onClick={rol=r.nombre},enabled=!s.cargando); Text(r.nombre)
            }
        }
        a.campos["rol"]?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Row(verticalAlignment=Alignment.CenterVertically) { Switch(activo,{activo=it},enabled=!s.cargando); Text(" Cuenta activa") }
        Text("Cambiar cuenta, rol o contrasena invalida los tokens anteriores. La cuenta debe iniciar sesion otra vez.",style=MaterialTheme.typography.bodySmall)
        Button(onClick={admin.guardarUsuario(id,FormularioUsuario(usuario.trim(),nombre.trim(),rol,password.ifBlank { null },activo))},enabled=!s.cargando && s.conectado) { Text("Guardar usuario") }
        if(id!=null && previo?.activo==true) OutlinedButton(onClick={desactivar=true},enabled=!s.cargando && s.conectado) { Text("Desactivar usuario") }
    }
    if(desactivar && id!=null) ConfirmarGestion("Desactivar usuario","La cuenta dejara de acceder y su historial se conservara.",{desactivar=false}) { desactivar=false;admin.desactivar(id) }
}
