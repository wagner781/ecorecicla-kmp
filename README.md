# EcoRutas

Kotlin Multiplatform (Android y JVM para pruebas), Compose Android, Java 21 y SDK 36.

Compilar: `./gradlew :androidApp:assembleDebug :shared:jvmTest`.

Se respeta el modelo de dominio entregado. SQLite/SQLDelight es la base local obligatoria del PDF; PostgreSQL pertenece al backend. El teléfono no necesita acceder a un servidor SQL para trabajar sin conexión.

El historial registra trabajo real de la sesión actual, sin fechas retroactivas.

## Ejecutar en Android

1. Inicia el backend del repositorio hermano con `docker compose up --build -d`.
2. Abre esta carpeta en Android Studio. Instala JDK 21 y SDK Android 36; configura `local.properties` con tu ruta al SDK (no se versiona).
3. Ejecuta `./gradlew :androidApp:assembleDebug :shared:allTests` (en Windows, `gradlew.bat`).
4. Ejecuta el módulo androidApp en el emulador, o instala `androidApp/build/outputs/apk/debug/androidApp-debug.apk` con `adb install -r`.
5. Entra con `operario1`, `operario2`, `supervisor1` o `admin` y contraseña `Eco2026*`. Operario y supervisor tienen ruta del día; el administrador abre el padron sin necesitar ruta propia y gestiona puntos, usuarios, roles y asignaciones. No registra pesajes.

La URL central está en BuildConfig, inyectada como ApiConfig. Por defecto es `http://10.0.2.2:8080/api/v1/`, acceso del emulador al equipo anfitrión. Para un dispositivo físico usa `-PecoApiUrl=http://IP-LAN-DEL-PC:8080/api/v1/` al compilar, y permite el puerto en tu firewall si lo necesitas. HTTP solo está habilitado en el manifiesto debug; release requiere un backend HTTPS y `-PecoApiUrl=https://.../api/v1/`. No publiques credenciales ni tokens.

## Arquitectura y contrato

`shared/commonMain`: `domain/model`, `domain/repository`, `domain/usecase`, `data/remote/dto`, `data/local`, `data/repository`, `presentation/listado`, `presentation/navigation` y `di`. El esquema común está en `shared/src/commonMain/sqldelight`. `androidApp` aporta AlmacenSeguroAndroid, BaseLocalAndroid, PlatformModule.android, actividad y UI Compose con tema propio.

La app usa Ktor 3.3.1, Koin 4.1.1, SQLDelight 2.1.0, coroutines/StateFlow, Compose Material3 y ViewModel. Comparte lógica con un target JVM para pruebas además del target Android. El esquema es SQLite local: PostgreSQL vive en el servicio. No hay una conexión SQL remota desde el teléfono y no hay listas de puntos incrustadas.

API `/api/v1`: POST `auth/login`, GET `me`, GET `rutas/dia`, GET `puntos/{id}/pesajes?pagina=0&tamanio=100`, POST `pesajes`, PUT/DELETE `pesajes/{id}`, POST `rutas/{id}/cierre`, PUT `rutas/{id}/puntos/{puntoId}/omision`. Los listados se deserializan como PaginaDto y se recorren hasta `ultima`; la ruta del día es un recurso compuesto. DELETE y omisión reciben `{motivo}` y devuelven 204 sin cuerpo. El neto no está en el DTO de escritura. Consulta el README del backend para el contrato completo.

La pantalla observa SQLite. Guardar funciona sin red, asigna IDs negativos y marca pendientes. Sincronizar envía antes de descargar; una clave UUID hace idempotente la creación. Los rechazos quedan visibles en la fila. Los pesajes de rutas anteriores se muestran en detalle sin contar para la atención del día. Omisiones y cierre requieren conexión; el registro y la corrección funcionan offline.

Operario: lectura, creación, corrección propia del día. Supervisor: esas capacidades, corrección global, anulación y cierre. Administrador: lectura y gestión de puntos, usuarios, roles y rutas desde la interfaz; sin escritura de pesajes. La interfaz ofrece acciones por privilegio, y el servicio las vuelve a verificar. El perfil muestra todos los privilegios.

## Pruebas y evidencias

`./gradlew :shared:allTests` ejecuta las pruebas commonTest (24 casos comunes y una prueba JVM de migracion en el cierre de Parte I) usando MockEngine y SQLite en memoria en JVM y variantes unitarias Android. Informe JVM: `shared/build/reports/tests/jvmTest/index.html`; Android: `shared/build/reports/tests/testDebugUnitTest/index.html`. La CI además genera un APK descargable.

`docs/evidencias/` contiene capturas reales de login, ruta, modo avión e ID definitivo después de sincronizar. [Guía de defensa](docs/DEFENSA.md): recorrido de los datos, seguridad, sincronización, decisiones y pasos de demostración. El cierre de sesión advierte que elimina también cambios sin enviar para dejar limpio el equipo compartido.

Git: ramas `main`, `develop` y `feature/...`. Bootstrap compilable como primer commit, PR hacia develop con merge commit y luego fusión de develop a main. No se falsifican sesiones pasadas. La Parte II se implementa por peticion del usuario en cuatro ramas SC. Este historial documenta trabajo real; no acredita una asignacion docente ni sesiones pasadas.

La sesion se administra mediante repositorio y casos de uso separados; `expiresAt` conserva el vencimiento firmado. Los cambios de rol invalidan tokens anteriores. Un 404 refresca el cache sin subir pendientes ni reintentar la escritura. La migracion SQLite 1→2 conserva la ruta anterior y agrega padron y rutas consultables.
