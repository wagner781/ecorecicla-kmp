# EcoRutas

Kotlin Multiplatform (Android y JVM para pruebas), Compose Android, Java 21 y SDK 36.

Compilar: `./gradlew :androidApp:assembleDebug :shared:jvmTest`.

Se respeta el modelo de dominio entregado. SQLite/SQLDelight es la base local obligatoria del PDF; PostgreSQL pertenece al backend. El teléfono no necesita acceder a un servidor SQL para trabajar sin conexión.

El historial registra trabajo real de la sesión actual, sin fechas retroactivas.
