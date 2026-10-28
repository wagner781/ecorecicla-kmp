package pe.upeu.ecorecicla.presentation

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
import androidx.compose.ui.unit.dp
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.presentation.administracion.*
import pe.upeu.ecorecicla.presentation.listado.*
import pe.upeu.ecorecicla.presentation.navigation.Pantalla

@Composable internal fun RolesScreen(s: UiState,a: AdminUiState,vm: EcoViewModel,admin: AdministracionViewModel) {
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("Roles y privilegios",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Button(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.RolFormulario())},enabled=!s.cargando && s.conectado) { Text("Nuevo rol") }
            OutlinedButton(onClick={admin.cargar(true)},enabled=!s.cargando && s.conectado) { Text("Actualizar roles") }
        }
        if(a.roles.isEmpty() && !s.cargando) item { Text("No hay roles cargados. Consulta con conexion.") }
        items(a.roles,key={it.nombre}) { r ->
            Card(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.RolFormulario(r.nombre))},modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(r.nombre,fontWeight=FontWeight.Bold)
                    Text(r.privilegios.sorted().joinToString(" · ").ifEmpty { "Solo acceso al perfil" },style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable internal fun RolFormularioScreen(s: UiState,a: AdminUiState,nombreOriginal: String?,vm: EcoViewModel,admin: AdministracionViewModel) {
    val previo=a.roles.find { it.nombre==nombreOriginal }
    var nombre by rememberSaveable(nombreOriginal) { mutableStateOf(previo?.nombre.orEmpty()) }
    var permisos by rememberSaveable(nombreOriginal) { mutableStateOf(previo?.privilegios?.toList() ?: emptyList()) }
    var eliminar by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.Roles)},enabled=!s.cargando) { Text("Volver a roles") }
        Text(if(nombreOriginal==null) "Nuevo rol" else "Editar privilegios",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        OutlinedTextField(nombre,{nombre=it.uppercase()},label={Text("Identificador del rol")},enabled=nombreOriginal==null && !s.cargando,singleLine=true,isError=a.campos["nombre"]!=null,supportingText={a.campos["nombre"]?.let { Text(it) }},modifier=Modifier.fillMaxWidth())
        a.privilegios.forEach { permiso -> Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(permiso in permisos,{checked -> permisos=if(checked) permisos+permiso else permisos-permiso},enabled=!s.cargando)
            Text(permiso,style=MaterialTheme.typography.bodyMedium)
        } }
        a.campos["privilegios"]?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Text("Los cambios invalidan las sesiones de los usuarios de este rol. El ultimo gestor activo debe conservar su acceso.",style=MaterialTheme.typography.bodySmall)
        Button(onClick={admin.guardarRol(nombreOriginal,FormularioRol(nombre.trim(),permisos.toSet()))},enabled=!s.cargando && s.conectado) { Text("Guardar rol") }
        if(nombreOriginal!=null) OutlinedButton(onClick={eliminar=true},enabled=!s.cargando && s.conectado) { Text("Eliminar rol") }
    }
    if(eliminar && nombreOriginal!=null) ConfirmarGestion("Eliminar rol","Solo se eliminan roles sin usuarios asignados.",{eliminar=false}) { eliminar=false;admin.eliminarRol(nombreOriginal) }
}
