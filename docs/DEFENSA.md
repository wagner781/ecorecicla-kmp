# Defensa de EcoRutas

## Flujo desde el servidor hasta la pantalla

`EcoApi.ruta()` usa el único HttpClient. Ktor deserializa `RutaDto`; el repositorio descarga las páginas de pesajes, sustituye el cache en una transacción SQLDelight y registra `ultimaSync`. `BaseLocal.puntos()` convierte la consulta `puntos` en Flow. `EcoViewModel` combina puntos, pesajes y ruta y publica `StateFlow<UiState>`. `EcoApp` observa el StateFlow con el ciclo de vida de Android. El composable no hace peticiones HTTP ni decide reglas del servidor.

El dominio conserva exactamente los tipos y la anulabilidad de PuntoAcopio, EstadoPesaje, Pesaje y Sesion del enunciado. No lleva anotaciones de serialización. `dto/Contratos.kt` tiene los contratos de red y sus conversiones. Los nombres JSON coinciden, por eso no se necesita un `@SerialName` adicional. El cliente tolera claves nuevas con `ignoreUnknownKeys`.

## Registro sin conexión

El caso de uso invoca `EcoRepositoryOffline.registrar`. Valida el formulario para dar respuesta inmediata, obtiene en transacción `MIN(COALESCE(MIN(id),0),0)-1`, asigna una clave UUID y guarda `pendiente=1`. El cálculo produce -1 incluso si no hay filas o solo hay IDs positivos. El peso neto provisional es 0 y se muestra como pendiente de cálculo del servidor. No se envía neto al API. Las correcciones conservan el neto anterior hasta que el servidor responda.

`puntos` calcula atención con EXISTS sobre pesajes de la ruta actual que no estén anulados; un registro local modifica el Flow sin esperar a la red. Los pesajes históricos se muestran en el detalle, pero no cuentan para la atención de hoy. El supervisor puede corregir registros de otras rutas: se conserva su ruta original y el servidor comprueba su cierre real, sin usar el estado de la ruta asignada al supervisor.

## Orden de sincronización

`SincronizarUseCase` delega en el repositorio. Un Mutex serializa sincronización, escritura y cierre de sesión. Primero se recorren los pendientes. CREAR hace POST, EDITAR hace PUT y ANULAR hace DELETE; el 204 no se deserializa. La sustitución del ID provisional y la fila definitiva es una transacción local. Luego se descarga la ruta y todas las páginas de cada punto. Solo cuando todas las respuestas llegan se sustituye el cache, también en transacción.

Si se descargara primero, se podría ocultar o reemplazar un registro local antes de enviarlo. Si el POST se guardó pero la respuesta se perdió, se reenvía la misma clave de creación y el servidor devuelve el mismo ID. Un 400, 403 o 409 conserva la fila y su mensaje; detiene la descarga para no destruir el cambio rechazado. La corrección del formulario conserva la clave. El usuario puede corregirlo o descartar expresamente el cambio local rechazado; esa acción no elimina nada del servidor.

`ConnectivityManager` emite un Flow al recuperar una red validada y el ViewModel solicita sincronización automática. También existe el botón manual. La reconexión automática funciona mientras el proceso de la app está activo y al reabrirla con red. No se promete sincronización con la app detenida por el sistema: eso requeriría WorkManager y otro ciclo de vida.

## Sesión compartida

`AlmacenSeguroAndroid` usa AES/GCM con un IV distinto en cada escritura. La clave AES está en Android Keystore; las preferencias solo contienen IV y datos cifrados. El token se conserva con su expiración, usuario, rol y privilegios. Al abrir se restaura únicamente una sesión vigente; las sesiones caducadas limpian el cache. No hay refresh token. `allowBackup=false` evita copiar el almacén de sesión/datos a otro equipo.

`ClienteHttp.defaultRequest` añade Authorization en un único sitio. Un 401 borra el token y el cache y el ViewModel vuelve al login; un 403 muestra el motivo y conserva la sesión. El cierre explícito limpia todo el turno, incluidos pendientes, con una confirmación que recomienda sincronizarlos antes. En primer plano se verifica además la vigencia cada 30 segundos y al volver a la actividad. El reloj local permite decidir si hay que mostrar login; la firma y la autorización se verifican realmente en el servicio.

## Errores y pantallas

`mapearError` traduce 400, 401, 403, 404, 409 y 5xx a la jerarquía ErrorApi. `llamada` distingue falta de conexión y timeout y respeta cancelación de corrutinas. UiState mantiene carga, contenido local, vacío, mensajes, errores de campo y disponibilidad de reintento. Si el servidor rechaza un pendiente con 400, el repositorio adjunta sus IDs al error de dominio; el ViewModel reabre el formulario correspondiente y conserva validationErrors bajo cada campo. El rechazo también queda en la fila. 404 y 403 no muestran reintento. Ante 404 se descarga de nuevo el cache sin enviar ni borrar cambios pendientes. Si el servidor borró un recurso con un cambio local pendiente, se conserva para resolverlo, en lugar de descartarlo automáticamente.

Los botones dependen de privilegios: registrar, corregir, anular y cerrar. Para operarios, corregir requiere además autor y fecha del día; para supervisor se usa el privilegio de alcance global de la matriz. Ocultarlos mejora la experiencia, pero no protege el servicio: las pruebas HTTP del backend llaman directamente con privilegios insuficientes y reciben 403.

## Inyección y pruebas

Koin registra `platformModule` (Android Keystore, SQLite driver, OkHttp, reloj, URL y conectividad), `redModule` (un HttpClient y EcoApi), `dataModule` (repositorio singleton), `domainModule` (casos de uso) y `presentationModule` (ViewModel). ViewModelProvider conserva el ViewModel al recrear la actividad. KMP comparte dominio, datos, SQL, casos de uso y estado de presentación; Android aporta Compose y dependencias de plataforma. JVM permite ejecutar commonTest en escritorio.

`ClienteTest` comprueba conversión, 403 conservando sesión y 401 borrándola. `OfflineTest` usa MockEngine y SQLite real en memoria: guarda sin ninguna llamada HTTP, crea IDs únicos, comprueba POST antes de GET, sustituye sin duplicar y conserva conflictos. También comprueba limpieza de turno. Las respuestas escritas en los tests son dobles de prueba; no hay datos de demostración incrustados en la app.

## Demostración sugerida

1. Levantar el backend y entrar como operario1.
2. Descargar la ruta y explicar `ultimaSync` y el origen SQLite de los puntos.
3. Activar modo avión, registrar un pesaje y mostrar ID negativo y marca pendiente.
4. Cerrar y volver a abrir la app: la sesión y el pendiente persisten.
5. Recuperar red, observar sustitución por ID positivo y verificar la única fila PostgreSQL.
6. Mostrar el perfil y ausencia de Anular/Cerrar; invocar DELETE con el mismo token por Postman para mostrar 403.
7. Entrar como supervisor y explicar las acciones disponibles, anulación lógica y RN-04.
8. Ejecutar `:shared:allTests` y abrir los informes. Recorrer un archivo de cada capa con un registro concreto.

Las capturas incluidas son reales de esta sesión. El historial no acredita desarrollo previo, no se fabricaron conflictos y el usuario solicito implementar las cuatro SC. Las ramas documentan avances reales desde develop; no prueban una asignacion docente durante el examen.

## Gestion y supervision completadas en Parte I

El administrador abre Padron, Usuarios, Roles o Asignacion. `AdministracionViewModel` usa casos de uso y repositorio propios; no recibe hashes ni decide por nombre de rol. Las selecciones de roles y privilegios provienen del API. Una asignacion admite solo usuarios activos con PESAJE_CREAR y conserva el orden de seleccion de los puntos. El servicio impide eliminar puntos referenciados, roles en uso y el ultimo administrador activo.

El supervisor selecciona rutas de operaciones por fecha, registra motivos de omision y solicita cierre. La respuesta 409 conserva la ruta abierta y explica cuantos puntos faltan. Tras el cierre se actualizan atomicamente el detalle y el listado local. `docs/evidencias/parte1` registra pruebas reales, incluidas altas, modificaciones y bajas de gestion. Las cuentas QA creadas por las pruebas quedan desactivadas.
## SC-A: busqueda y pendientes

La ruta combina busqueda por razon social, direccion, zona o material con «Solo pendientes de pesar». `FiltrarRutaUseCase` conserva el orden planificado y recibe las filas observables de SQLite: funciona sin red y reacciona a un pesaje local. Una omision justificada resuelve la visita y queda fuera del filtro, aunque siga visible al desactivarlo. El composable solo dibuja `puntosVisibles` y emite eventos. Pruebas: FiltroRutaTest cubre combinacion, orden, vacio y cambio del cache con conectividad falsa.

## SC-B: acceso de consulta

`auditor1 / Eco2026*` entra al padron y consulta puntos, pesajes y perfil. No tiene creacion, correccion, anulacion, cierre ni gestion. La interfaz y los casos de uso dependen de privilegios; cambiar el nombre del rol no cambia la autorizacion. `AuditorClienteTest` garantiza que no puede encolar escrituras ni llamar al cierre y conserva la sesion ante rechazo. El servidor repite la autorizacion. Captura real: docs/evidencias/parte2/sc-b-auditor.png.

## SC-C: humedad estimada de extremo a extremo

El formulario y el detalle muestran el mismo porcentaje opcional, 0..100 con dos decimales. Vacio conserva null; un texto invalido produce error de campo. `PesajePeticionDto` envia el valor; `PesajeDto.dominio()` lo conserva y `BaseLocal` lo persiste antes de sincronizar. La migracion SQLite 2→3 agrega una columna nullable sin perder IDs provisionales ni claves. No cambia el peso neto ni se calcula descuento por humedad: esa regla no fue solicitada. HumedadClienteTest verifica guardado sin red, cuerpo HTTP, respuesta definitiva y validacion; MigracionTest conserva un pendiente antiguo. Se probaron 30 casos comunes y dos migraciones JVM.
