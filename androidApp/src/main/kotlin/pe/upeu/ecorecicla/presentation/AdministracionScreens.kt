package pe.upeu.ecorecicla.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.presentation.administracion.*
import pe.upeu.ecorecicla.presentation.listado.*
import pe.upeu.ecorecicla.presentation.navigation.Pantalla

@Composable internal fun AdministracionScreen(s: UiState,a: AdminUiState,vm: EcoViewModel,admin: AdministracionViewModel) {
    when(val p=s.pantalla) {
        is Pantalla.PuntoFormulario -> PuntoFormulario(s,a,p.id,vm,admin)
        Pantalla.Usuarios -> UsuariosScreen(s,a,vm,admin)
        is Pantalla.UsuarioFormulario -> UsuarioFormularioScreen(s,a,p.id,vm,admin)
        else -> Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Administracion",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("Mantiene el padron y la planificacion. Este privilegio no permite registrar, corregir ni anular pesajes.")
            Button(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.PuntoFormulario())},enabled=!s.cargando && s.conectado) { Text("Nuevo punto de acopio") }
            Button(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.Usuarios)},enabled=!s.cargando) { Text("Usuarios") }
            OutlinedButton(onClick={vm.navegar(Pantalla.Rutas)},enabled=!s.cargando) { Text("Ver rutas") }
        }
    }
}

@Composable private fun PuntoFormulario(s: UiState,a: AdminUiState,id: Long?,vm: EcoViewModel,admin: AdministracionViewModel) {
    val previo=s.padron.find { it.id==id }
    var razon by rememberSaveable(id) { mutableStateOf(previo?.razonSocial.orEmpty()) }
    var direccion by rememberSaveable(id) { mutableStateOf(previo?.direccion.orEmpty()) }
    var zona by rememberSaveable(id) { mutableStateOf(previo?.zona.orEmpty()) }
    var material by rememberSaveable(id) { mutableStateOf(previo?.materialHabitual.orEmpty()) }
    var eliminar by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.Padron)},enabled=!s.cargando) { Text("Volver al padron") }
        Text(if(id==null) "Nuevo punto de acopio" else "Editar punto",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Campo("Razon social",razon,{razon=it},a.campos["razonSocial"])
        Campo("Direccion",direccion,{direccion=it},a.campos["direccion"])
        Campo("Zona",zona,{zona=it},a.campos["zona"])
        Campo("Material habitual",material,{material=it},a.campos["materialHabitual"])
        if(!s.conectado) Text("La gestion del padron requiere conexion")
        Button(onClick={admin.guardarPunto(id,FormularioPunto(razon.trim(),direccion.trim(),zona.trim(),material.trim()))},enabled=!s.cargando && s.conectado) { Text("Guardar punto") }
        if(id!=null) OutlinedButton(onClick={eliminar=true},enabled=!s.cargando && s.conectado) { Text("Eliminar punto") }
    }
    if(eliminar && id!=null) ConfirmarGestion("Eliminar punto","Solo se eliminan puntos sin rutas asignadas. Los puntos con historial se conservan.",{eliminar=false}) { eliminar=false;admin.eliminarPunto(id) }
}

@Composable internal fun ConfirmarGestion(titulo: String,mensaje: String,cancelar: () -> Unit,aceptar: () -> Unit) {
    AlertDialog(onDismissRequest=cancelar,title={Text(titulo)},text={Text(mensaje)},confirmButton={TextButton(onClick=aceptar) { Text("Confirmar") }},dismissButton={TextButton(onClick=cancelar) { Text("Cancelar") }})
}
