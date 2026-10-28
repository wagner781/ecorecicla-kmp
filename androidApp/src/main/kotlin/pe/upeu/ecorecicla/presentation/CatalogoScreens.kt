package pe.upeu.ecorecicla.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.upeu.ecorecicla.presentation.listado.*
import pe.upeu.ecorecicla.presentation.navigation.Pantalla

@Composable internal fun Padron(s: UiState,vm: EcoViewModel) {
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("Puntos de acopio",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("Consulta el padron descargado, incluso sin conexion.")
            Campo("Filtrar por zona",s.zona,vm::filtrarZona,null)
            Button(onClick=vm::sincronizar,enabled=!s.cargando) { Text("Actualizar padron") }
        }
        if(s.padronVisible.isEmpty() && !s.cargando) item { Text(if(s.zona.isBlank()) "No hay puntos descargados. Actualiza con conexion." else "No hay puntos para esa zona.") }
        items(s.padronVisible,key={it.id}) { p ->
            Card(onClick={vm.navegar(Pantalla.Detalle(p.id))},modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(p.razonSocial,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Text(p.direccion); Text("${p.zona} · ${p.materialHabitual}")
                }
            }
        }
    }
}

@Composable internal fun Rutas(s: UiState,vm: EcoViewModel) {
    var fecha by rememberSaveable { mutableStateOf(s.fechaRutas) }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("Rutas de operaciones",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Campo("Fecha (AAAA-MM-DD)",fecha,{fecha=it},s.campos["fecha"])
            Button(onClick={vm.cargarRutas(fecha)},enabled=!s.cargando && s.conectado) { Text("Consultar fecha") }
            Text("Selecciona una ruta para ver sus puntos y, si tienes permiso, cerrarla. Resuelve los pendientes antes de cambiar.")
        }
        if(s.rutasVisibles.isEmpty() && !s.cargando) item { Text("No hay rutas descargadas para ${s.fechaRutas}.") }
        items(s.rutasVisibles,key={it.id}) { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text("Ruta #${r.id} · ${r.usuario}",fontWeight=FontWeight.Bold)
                    Text("${r.fecha} · ${if(r.cerrada) "Cerrada" else "Abierta"}")
                    Button(onClick={vm.seleccionarRuta(r.id)},enabled=!s.cargando && s.conectado) { Text(if(s.ruta?.id==r.id) "Abrir ruta seleccionada" else "Seleccionar ruta") }
                }
            }
        }
    }
}
