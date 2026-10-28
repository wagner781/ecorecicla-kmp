package pe.upeu.ecorecicla.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import pe.upeu.ecorecicla.presentation.administracion.AdministracionViewModel
import java.time.LocalDate
import java.time.ZoneId

@Composable fun EcoApp(vm: EcoViewModel,admin: AdministracionViewModel) {
    val principal by vm.state.collectAsStateWithLifecycle()
    val a by admin.state.collectAsStateWithLifecycle()
    val s=principal.copy(cargando=principal.cargando || a.cargando)
    val gestor=s.sesion?.privilegios?.contains("PUNTO_GESTIONAR")==true
    LaunchedEffect(s.sesion?.token) { admin.limpiar() }
    LaunchedEffect(s.pantalla,s.sesion?.token) { if(gestor && s.pantalla !in listOf(Pantalla.Ruta,Pantalla.Padron,Pantalla.Rutas,Pantalla.Perfil)) admin.cargar() }
    LaunchedEffect(a.destino) {
        a.destino?.let { destino ->
            if(destino==Pantalla.Rutas && a.fechaDestino!=null) vm.abrirRutasGuardadas(a.fechaDestino!!)
            else vm.navegar(destino)
            admin.consumirDestino()
        }
    }
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
                        Column(Modifier.padding(12.dp)) { Text(message); if(s.reintentar) TextButton(onClick=vm::reintentar,enabled=!s.cargando) { Text("Reintentar") } }
                    }
                }
                if(gestor) a.mensaje?.let { message -> Card(Modifier.fillMaxWidth().padding(vertical=8.dp)) {
                    Column(Modifier.padding(12.dp)) { Text(message); if(a.reintentar) TextButton(onClick=admin::reintentar,enabled=!s.cargando) { Text("Reintentar") } }
                } }
                if(s.sesion==null) Login(s,vm)
                else {
                    if(s.sesion?.soloLectura==true) Text("Acceso de consulta: puntos y pesajes",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        TextButton(onClick={ vm.navegar(Pantalla.Ruta) },enabled=!s.cargando) { Text("Mi ruta") }
                        TextButton(onClick={ vm.navegar(Pantalla.Padron) },enabled=!s.cargando) { Text("Puntos") }
                        if(s.sesion?.privilegios?.any { it=="RUTA_CERRAR" || it=="PUNTO_GESTIONAR" }==true) TextButton(onClick={vm.navegar(Pantalla.Rutas)},enabled=!s.cargando) { Text("Rutas") }
                        if(gestor) TextButton(onClick={admin.limpiarMensaje();vm.navegar(Pantalla.Administracion)},enabled=!s.cargando) { Text("Administrar") }
                        TextButton(onClick=vm::perfil,enabled=!s.cargando) { Text("Perfil") }
                    }
                    BackHandler(enabled=s.pantalla!=Pantalla.Ruta) {
                        val screen=s.pantalla
                        vm.navegar(if(screen is Pantalla.Formulario) Pantalla.Detalle(screen.puntoId) else Pantalla.Ruta)
                    }
                    when(val screen=s.pantalla) {
                        Pantalla.Ruta -> Ruta(s,vm)
                        Pantalla.Perfil -> Perfil(s,vm)
                        Pantalla.Padron -> Padron(s,vm)
                        Pantalla.Rutas -> Rutas(s,vm)
                        is Pantalla.Detalle -> Detalle(s,screen.puntoId,vm)
                        is Pantalla.Formulario -> Formulario(s,screen,vm)
                        else -> if(gestor) AdministracionScreen(s,a,vm,admin) else Text("No tienes permiso para esta pantalla")
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
            s.ruta?.let { r -> Text("Ruta #${r.id} · ${s.rutas.find { it.id==r.id }?.usuario ?: "Usuario #${r.usuarioId}"}",style=MaterialTheme.typography.bodySmall) }
            if(s.sesion?.privilegios?.contains("PUNTO_GESTIONAR")==true && s.ruta?.cerrada==false) TextButton(onClick={vm.navegar(Pantalla.Asignacion(s.ruta?.id))},enabled=!s.cargando) { Text("Editar planificacion") }
            Text("Ultima sincronizacion: ${s.ruta?.ultimaSync ?: "Todavia no se sincroniza"}",style=MaterialTheme.typography.bodySmall)
            Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.small) {
                Text("Pendientes de sincronizar: ${s.pendientesSync}",modifier=Modifier.padding(8.dp),style=MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick=vm::sincronizar,enabled=!s.cargando) { Text("Sincronizar") }
            if(s.sesion?.privilegios?.contains("RUTA_CERRAR")==true && s.ruta?.cerrada==false) {
                var confirmar by remember { mutableStateOf(false) }
                OutlinedButton(onClick={confirmar=true},enabled=!s.cargando) { Text("Cerrar ruta") }
                if(confirmar) AlertDialog(onDismissRequest={confirmar=false},title={Text("Cerrar ruta")},text={Text("El servidor verificara que todos los puntos tengan pesaje u omision. El cierre impide nuevos registros.")},confirmButton={ TextButton(onClick={confirmar=false;vm.cerrarRuta()}) { Text("Cerrar") } },dismissButton={TextButton(onClick={confirmar=false}) { Text("Cancelar") }})
            }
        }
        if(s.puntos.isEmpty() && !s.cargando) item { Text("Aun no hay puntos. Sincroniza con conexion para descargar tu ruta.") }
        item {
            OutlinedTextField(value=s.busquedaRuta,onValueChange=vm::buscarRuta,label={Text("Buscar en mi ruta")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Row(verticalAlignment=Alignment.CenterVertically) {
                Switch(checked=s.soloPendientes,onCheckedChange=vm::mostrarSoloPendientes)
                Text("Solo pendientes de pesar",modifier=Modifier.padding(start=8.dp))
            }
            if(s.puntos.isNotEmpty() && s.puntosVisibles.isEmpty()) Text("No hay puntos que coincidan con los filtros.")
        }
        items(s.puntosVisibles,key={it.id}) { p ->
            Card(onClick={vm.navegar(Pantalla.Detalle(p.id))},modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("${p.secuencia.toString().padStart(2,'0')}  ${p.razonSocial}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Text(p.direccion); Text("${p.zona} · ${p.materialHabitual}",style=MaterialTheme.typography.bodySmall)
                    Text(when { p.atendido -> "Con pesaje"; !p.motivoOmision.isNullOrBlank() -> "Omitido: ${p.motivoOmision}"; else -> "Pendiente de pesar" },color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable private fun Detalle(s: UiState,id: Long,vm: EcoViewModel) {
    val p=s.puntos.find { it.id==id } ?: s.padron.find { it.id==id }
    val registros=s.pesajes.filter { it.pesaje.puntoId==id }
    var anulando by remember { mutableStateOf<Long?>(null) }
    var omision by remember { mutableStateOf(false) }
    var descartando by remember { mutableStateOf<Long?>(null) }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick={vm.navegar(Pantalla.Ruta)}) { Text("Volver a mi ruta") }
            Text(p?.razonSocial ?: "Punto no disponible",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text(p?.direccion.orEmpty()); Text("${p?.zona.orEmpty()} · ${p?.materialHabitual.orEmpty()}")
            p?.motivoOmision?.let { Text("Omision: $it") }
            if(s.sesion?.privilegios?.contains("PUNTO_GESTIONAR")==true) OutlinedButton(onClick={vm.navegar(Pantalla.PuntoFormulario(id))},enabled=!s.cargando) { Text("Editar punto") }
            if(s.sesion?.privilegios?.contains("PESAJE_CREAR")==true && s.ruta?.cerrada==false && s.puntos.any { it.id==id }) {
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
                    Text("Humedad estimada: ${w.humedadEstimada?.let { "$it %" } ?: "Sin estimar"}")
                    Text("${w.operario} · ${w.fechaHora}",style=MaterialTheme.typography.bodySmall)
                    if(w.pendiente) Text("Pendiente de sincronizar",color=MaterialTheme.colorScheme.primary)
                    (w.estado as? EstadoPesaje.Anulado)?.let { Text("Anulado: ${it.motivo}",color=MaterialTheme.colorScheme.error) }
                    r.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                    val ownToday=w.operario==s.sesion?.usuario && w.fechaHora.substringBefore('T')==LocalDate.now(ZoneId.of("America/Lima")).toString()
                    val global=s.sesion?.privilegios?.contains("PESAJE_ANULAR")==true
                    val current=if(r.rutaId==s.ruta?.id) s.ruta?.cerrada==false else global
                    if(w.estado==EstadoPesaje.Registrado) {
                        if(current && s.sesion?.privilegios?.contains("PESAJE_EDITAR")==true && (ownToday || s.sesion?.privilegios?.contains("PESAJE_ANULAR")==true)) TextButton(onClick={vm.navegar(Pantalla.Formulario(id,w.id))},enabled=!s.cargando) { Text("Corregir") }
                        if(s.sesion?.privilegios?.contains("PESAJE_ANULAR")==true) TextButton(onClick={anulando=w.id},enabled=!s.cargando) { Text("Anular") }
                    }
                    if(w.pendiente && r.error!=null) TextButton(onClick={descartando=w.id},enabled=!s.cargando) { Text("Descartar cambio local") }
                }
            }
        }
    }
    anulando?.let { pesaje -> MotivoDialog("Anular pesaje",{anulando=null}) { motivo -> anulando=null;vm.anular(pesaje,motivo) } }
    if(omision) MotivoDialog("Motivo de omision",{omision=false}) { motivo -> omision=false;vm.omitir(id,motivo) }
    descartando?.let { pesaje -> AlertDialog(onDismissRequest={descartando=null},title={Text("Descartar cambio local")},text={Text("Se perdera solo este cambio sin enviar. Los registros del servidor se conservan y se descargaran al sincronizar.")},confirmButton={TextButton(onClick={descartando=null;vm.descartar(pesaje)}) { Text("Descartar") }},dismissButton={TextButton(onClick={descartando=null}) { Text("Cancelar") }}) }
}

@Composable private fun Formulario(s: UiState,screen: Pantalla.Formulario,vm: EcoViewModel) {
    val previo=s.pesajes.find { it.pesaje.id==screen.pesajeId }
    var material by rememberSaveable(screen.pesajeId,screen.puntoId) { mutableStateOf(previo?.pesaje?.material ?: s.puntos.find { it.id==screen.puntoId }?.materialHabitual.orEmpty()) }
    var bruto by rememberSaveable(screen.pesajeId,screen.puntoId) { mutableStateOf(previo?.pesaje?.pesoBruto?.toString().orEmpty()) }
    var tara by rememberSaveable(screen.pesajeId,screen.puntoId) { mutableStateOf(previo?.pesaje?.tara?.toString() ?: "0") }
    var humedad by rememberSaveable(screen.pesajeId,screen.puntoId) { mutableStateOf(previo?.pesaje?.humedadEstimada?.toString().orEmpty()) }
    var observacion by rememberSaveable(screen.pesajeId,screen.puntoId) { mutableStateOf(previo?.observacion.orEmpty()) }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={vm.navegar(Pantalla.Detalle(screen.puntoId))}) { Text("Volver al punto") }
        Text(if(screen.pesajeId==null) "Registrar pesaje" else "Corregir pesaje",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Se guarda primero en este dispositivo. El servidor calcula el peso neto al sincronizar.")
        Campo("Material",material,{material=it},s.campos["material"])
        Campo("Peso bruto (kg)",bruto,{bruto=it},s.campos["pesoBruto"],true)
        Campo("Tara (kg)",tara,{tara=it},s.campos["tara"],true)
        Campo("Humedad estimada (%) - opcional",humedad,{humedad=it},s.campos["humedadEstimada"],true)
        Campo("Observacion",observacion,{observacion=it},s.campos["observacion"])
        Button(onClick={ vm.guardar(FormularioPesaje(screen.puntoId,material,bruto.replace(',','.').toDoubleOrNull() ?: Double.NaN,tara.replace(',','.').toDoubleOrNull() ?: Double.NaN,observacion.ifBlank { null },if(humedad.isBlank()) null else humedad.replace(',','.').toDoubleOrNull() ?: Double.NaN),screen.pesajeId) },enabled=!s.cargando,modifier=Modifier.fillMaxWidth().height(52.dp)) { Text("Guardar pesaje") }
    }
}

@Composable private fun Perfil(s: UiState,vm: EcoViewModel) {
    var confirmar by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Tu perfil",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(s.sesion?.nombre.orEmpty(),style=MaterialTheme.typography.titleLarge)
        Text("Usuario: ${s.sesion?.usuario.orEmpty()}")
        Text(s.sesion?.rol.orEmpty())
        Text("Privilegios",fontWeight=FontWeight.Bold)
        s.sesion?.privilegios?.sorted()?.forEach { Text(it) }
        OutlinedButton(onClick={confirmar=true},enabled=!s.cargando) { Text("Cerrar sesion") }
    }
    if(confirmar) AlertDialog(onDismissRequest={confirmar=false},title={Text("Cerrar turno")},text={Text("Se borraran la sesion y los datos de este dispositivo, incluidos los cambios sin enviar. Sincroniza antes si necesitas conservarlos.")},confirmButton={TextButton(onClick={confirmar=false;vm.salir()}) { Text("Cerrar sesion y limpiar") }},dismissButton={TextButton(onClick={confirmar=false}) { Text("Cancelar") }})
}

@Composable internal fun Campo(label: String,value: String,onChange: (String)->Unit,error: String?,decimal: Boolean=false) {
    OutlinedTextField(value,onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,isError=error!=null,supportingText={if(error!=null) Text(error)},keyboardOptions=KeyboardOptions(keyboardType=if(decimal) KeyboardType.Decimal else KeyboardType.Text))
}

@Composable private fun MotivoDialog(title: String,onDismiss: ()->Unit,onConfirm: (String)->Unit) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Campo("Motivo",motivo,{motivo=it},null)},confirmButton={TextButton(onClick={onConfirm(motivo)},enabled=motivo.isNotBlank()) { Text("Confirmar") }},dismissButton={TextButton(onClick=onDismiss) { Text("Cancelar") }})
}
