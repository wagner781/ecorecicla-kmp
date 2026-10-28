package pe.upeu.ecorecicla.presentation

import androidx.compose.foundation.layout.*
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

@Composable internal fun AsignacionScreen(s: UiState,a: AdminUiState,id: Long?,vm: EcoViewModel,admin: AdministracionViewModel) {
    val previo=s.ruta?.takeIf { it.id==id }
    var fecha by rememberSaveable(id) { mutableStateOf(previo?.fecha ?: s.fechaRutas) }
    var usuarioId by rememberSaveable(id) { mutableLongStateOf(previo?.usuarioId ?: 0) }
    var puntos by rememberSaveable(id) { mutableStateOf(if(previo==null) emptyList<Long>() else s.puntos.map { it.id }) }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.Administracion)},enabled=!s.cargando) { Text("Volver a administracion") }
        Text(if(id==null) "Asignar ruta" else "Editar planificacion",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Campo("Fecha (AAAA-MM-DD)",fecha,{fecha=it},a.campos["fecha"])
        Text("Usuario de la ruta",fontWeight=FontWeight.Bold)
        a.asignables.forEach { u -> Row(verticalAlignment=Alignment.CenterVertically) {
            RadioButton(usuarioId==u.id,{usuarioId=u.id},enabled=!s.cargando); Text("${u.usuario} · ${u.nombre}")
        } }
        if(a.asignables.isEmpty() && !s.cargando) Text("No hay usuarios activos con permiso para registrar pesajes")
        a.campos["usuarioId"]?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Text("Puntos en orden de visita",fontWeight=FontWeight.Bold)
        Text("La secuencia sigue el orden en que marcas los puntos. Desmarca y vuelve a marcar para cambiar el orden.",style=MaterialTheme.typography.bodySmall)
        s.padron.forEach { p -> Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(p.id in puntos,{checked -> puntos=if(checked) puntos+p.id else puntos-p.id},enabled=!s.cargando)
            Text((if(p.id in puntos) "${puntos.indexOf(p.id)+1}. " else "")+p.razonSocial)
        } }
        if(s.padron.isEmpty()) Text("Primero actualiza el padron")
        a.campos["puntoIds"]?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Text("Se conserva una sola ruta por usuario y fecha. Las rutas con pesajes u omisiones mantienen su planificacion.",style=MaterialTheme.typography.bodySmall)
        Button(onClick={admin.guardarRuta(id,FormularioRuta(usuarioId,fecha.trim(),puntos))},enabled=!s.cargando && s.conectado) { Text("Guardar asignacion") }
    }
}
