package pe.upeu.ecorecicla.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.upeu.ecorecicla.presentation.listado.*
import pe.upeu.ecorecicla.presentation.navigation.Pantalla
import pe.upeu.ecorecicla.presentation.theme.EcoTheme
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import java.time.LocalDate
import java.time.ZoneId

@Composable fun EcoApp(vm: EcoViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    EcoTheme {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal=20.dp,vertical=12.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column { Text("EcoRutas",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold); Text("Cada recojo cuenta",style=MaterialTheme.typography.bodySmall) }
                    if(s.sesion!=null) Text(if(s.conectado) "Con conexion" else "Sin conexion",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(12.dp))
                if(s.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
                s.mensaje?.let { message ->
                    Card(Modifier.fillMaxWidth().padding(vertical=8.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(12.dp)) { Text(message); if(s.reintentar && s.sesion!=null) TextButton(onClick=vm::sincronizar,enabled=!s.cargando) { Text("Reintentar") } }
                    }
                }
                if(s.sesion==null) Login(s,vm)
                else {
                    Row {
                        TextButton(onClick={ vm.navegar(Pantalla.Ruta) },enabled=!s.cargando) { Text("Mi ruta") }
                        TextButton(onClick=vm::perfil,enabled=!s.cargando) { Text("Perfil") }
                    }
                    BackHandler(enabled=s.pantalla!=Pantalla.Ruta) {
                        val screen=s.pantalla
                        vm.navegar(if(screen is Pantalla.Formulario) Pantalla.Detalle(screen.puntoId) else Pantalla.Ruta)
                    }
                    when(val screen=s.pantalla) {
                        Pantalla.Ruta -> Ruta(s,vm)
                        Pantalla.Perfil -> Perfil(s,vm)
                        is Pantalla.Detalle -> Detalle(s,screen.puntoId,vm)
                        is Pantalla.Formulario -> Formulario(s,screen,vm)
                    }
                }
            }
        }
    }
}

@Composable private fun Login(s: UiState,vm: EcoViewModel) {
    var usuario by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top=40.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Empieza tu recorrido",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Inicia sesion para descargar tu ruta. Luego podras registrar pesajes incluso sin cobertura.")
        Campo("Usuario",usuario,{usuario=it},s.campos["usuario"])
        OutlinedTextField(password,{password=it},label={Text("Contrasena")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!s.cargando)
        Button(onClick={vm.login(usuario.trim(),password)},enabled=!s.cargando && usuario.isNotBlank() && password.isNotBlank(),modifier=Modifier.fillMaxWidth().height(52.dp)) { Text(if(s.cargando) "Ingresando..." else "Iniciar sesion") }
    }
}

@Composable private fun Ruta(s: UiState,vm: EcoViewModel) {
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("Ruta del dia",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text(s.ruta?.let { "${it.fecha} · ${if(it.cerrada) "Cerrada" else "Abierta"}" } ?: "Sin ruta descargada")
            Text("Ultima sincronizacion: ${s.ruta?.ultimaSync ?: "Todavia no se sincroniza"}",style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Button(onClick=vm::sincronizar,enabled=!s.cargando) { Text("Sincronizar") }
            if(s.sesion?.privilegios?.contains("RUTA_CERRAR")==true && s.ruta?.cerrada==false) {
                var confirmar by remember { mutableStateOf(false) }
                OutlinedButton(onClick={confirmar=true},enabled=!s.cargando) { Text("Cerrar ruta") }
                if(confirmar) AlertDialog(onDismissRequest={confirmar=false},title={Text("Cerrar ruta")},text={Text("El servidor verificara que todos los puntos tengan pesaje u omision. El cierre impide nuevos registros.")},confirmButton={ TextButton(onClick={confirmar=false;vm.cerrarRuta()}) { Text("Cerrar") } },dismissButton={TextButton(onClick={confirmar=false}) { Text("Cancelar") }})
            }
        }
        if(s.puntos.isEmpty() && !s.cargando) item { Text("Aun no hay puntos. Sincroniza con conexion para descargar tu ruta.") }
        items(s.puntos,key={it.id}) { p ->
            Card(onClick={vm.navegar(Pantalla.Detalle(p.id))},modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("${p.secuencia.toString().padStart(2,'0')}  ${p.razonSocial}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Text(p.direccion); Text("${p.zona} · ${p.materialHabitual}",style=MaterialTheme.typography.bodySmall)
                    Text(if(p.atendido) "Con pesaje" else "Pendiente de pesar",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable private fun Detalle(s: UiState,id: Long,vm: EcoViewModel) {
    val p=s.puntos.find { it.id==id }
    val registros=s.pesajes.filter { it.pesaje.puntoId==id }
    var anulando by remember { mutableStateOf<Long?>(null) }
    var omision by remember { mutableStateOf(false) }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick={vm.navegar(Pantalla.Ruta)}) { Text("Volver a mi ruta") }
            Text(p?.razonSocial ?: "Punto no disponible",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text(p?.direccion.orEmpty()); Text("${p?.zona.orEmpty()} · ${p?.materialHabitual.orEmpty()}")
            if(s.sesion?.privilegios?.contains("PESAJE_CREAR")==true && s.ruta?.cerrada==false) {
                Button(onClick={vm.navegar(Pantalla.Formulario(id))},enabled=!s.cargando) { Text("Registrar pesaje") }
                TextButton(onClick={omision=true},enabled=s.conectado && !s.cargando) { Text("Registrar motivo de omision") }
            }
            Text("Pesajes",style=MaterialTheme.typography.titleLarge)
        }
        if(registros.isEmpty()) item { Text("Este punto aun no tiene pesajes guardados.") }
        items(registros,key={it.pesaje.id}) { r ->
            val w=r.pesaje
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("${w.material} · #${w.id}",fontWeight=FontWeight.Bold)
                    Text("Bruto ${w.pesoBruto} kg · Tara ${w.tara} kg")
                    Text(if(w.id<0) "Neto pendiente del servidor" else "Neto ${w.pesoNeto} kg")
                    Text("${w.operario} · ${w.fechaHora}",style=MaterialTheme.typography.bodySmall)
                    if(w.pendiente) Text("Pendiente de sincronizar",color=MaterialTheme.colorScheme.primary)
                    (w.estado as? EstadoPesaje.Anulado)?.let { Text("Anulado: ${it.motivo}",color=MaterialTheme.colorScheme.error) }
                    r.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                    val ownToday=w.operario==s.sesion?.usuario && w.fechaHora.substringBefore('T')==LocalDate.now(ZoneId.of("America/Lima")).toString()
                    val current=r.rutaId==s.ruta?.id && s.ruta?.cerrada==false
                    if(current && w.estado==EstadoPesaje.Registrado) {
                        if(s.sesion?.privilegios?.contains("PESAJE_EDITAR")==true && (ownToday || s.sesion?.privilegios?.contains("PESAJE_ANULAR")==true)) TextButton(onClick={vm.navegar(Pantalla.Formulario(id,w.id))},enabled=!s.cargando) { Text("Corregir") }
                        if(s.sesion?.privilegios?.contains("PESAJE_ANULAR")==true) TextButton(onClick={anulando=w.id},enabled=!s.cargando) { Text("Anular") }
                    }
                }
            }
        }
    }
    anulando?.let { pesaje -> MotivoDialog("Anular pesaje",{anulando=null}) { motivo -> anulando=null;vm.anular(pesaje,motivo) } }
    if(omision) MotivoDialog("Motivo de omision",{omision=false}) { motivo -> omision=false;vm.omitir(id,motivo) }
}

@Composable private fun Formulario(s: UiState,screen: Pantalla.Formulario,vm: EcoViewModel) {
    val previo=s.pesajes.find { it.pesaje.id==screen.pesajeId }
    var material by rememberSaveable(screen.pesajeId,screen.puntoId) { mutableStateOf(previo?.pesaje?.material ?: s.puntos.find { it.id==screen.puntoId }?.materialHabitual.orEmpty()) }
    var bruto by rememberSaveable(screen.pesajeId,screen.puntoId) { mutableStateOf(previo?.pesaje?.pesoBruto?.toString().orEmpty()) }
    var tara by rememberSaveable(screen.pesajeId,screen.puntoId) { mutableStateOf(previo?.pesaje?.tara?.toString() ?: "0") }
    var observacion by rememberSaveable(screen.pesajeId,screen.puntoId) { mutableStateOf(previo?.observacion.orEmpty()) }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={vm.navegar(Pantalla.Detalle(screen.puntoId))}) { Text("Volver al punto") }
        Text(if(screen.pesajeId==null) "Registrar pesaje" else "Corregir pesaje",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Se guarda primero en este dispositivo. El servidor calcula el peso neto al sincronizar.")
        Campo("Material",material,{material=it},s.campos["material"])
        Campo("Peso bruto (kg)",bruto,{bruto=it},s.campos["pesoBruto"],true)
        Campo("Tara (kg)",tara,{tara=it},s.campos["tara"],true)
        Campo("Observacion",observacion,{observacion=it},s.campos["observacion"])
        Button(onClick={ vm.guardar(FormularioPesaje(screen.puntoId,material,bruto.replace(',','.').toDoubleOrNull() ?: Double.NaN,tara.replace(',','.').toDoubleOrNull() ?: Double.NaN,observacion.ifBlank { null }),screen.pesajeId) },enabled=!s.cargando,modifier=Modifier.fillMaxWidth().height(52.dp)) { Text("Guardar pesaje") }
    }
}

@Composable private fun Perfil(s: UiState,vm: EcoViewModel) {
    var confirmar by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Tu perfil",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(s.sesion?.usuario.orEmpty(),style=MaterialTheme.typography.titleLarge)
        Text(s.sesion?.rol.orEmpty())
        Text("Privilegios",fontWeight=FontWeight.Bold)
        s.sesion?.privilegios?.sorted()?.forEach { Text(it) }
        OutlinedButton(onClick={confirmar=true},enabled=!s.cargando) { Text("Cerrar sesion") }
    }
    if(confirmar) AlertDialog(onDismissRequest={confirmar=false},title={Text("Cerrar turno")},text={Text("Se borraran la sesion y los datos de este dispositivo, incluidos los cambios sin enviar. Sincroniza antes si necesitas conservarlos.")},confirmButton={TextButton(onClick={confirmar=false;vm.salir()}) { Text("Cerrar sesion y limpiar") }},dismissButton={TextButton(onClick={confirmar=false}) { Text("Cancelar") }})
}

@Composable private fun Campo(label: String,value: String,onChange: (String)->Unit,error: String?,decimal: Boolean=false) {
    OutlinedTextField(value,onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,isError=error!=null,supportingText={if(error!=null) Text(error)},keyboardOptions=KeyboardOptions(keyboardType=if(decimal) KeyboardType.Decimal else KeyboardType.Text))
}

@Composable private fun MotivoDialog(title: String,onDismiss: ()->Unit,onConfirm: (String)->Unit) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Campo("Motivo",motivo,{motivo=it},null)},confirmButton={TextButton(onClick={onConfirm(motivo)},enabled=motivo.isNotBlank()) { Text("Confirmar") }},dismissButton={TextButton(onClick=onDismiss) { Text("Cancelar") }})
}
