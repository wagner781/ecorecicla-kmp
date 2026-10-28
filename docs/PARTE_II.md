# Las cuatro solicitudes: implementacion y defensa

Se implementan las cuatro opciones por solicitud del usuario. Esto sirve para practicar; no acredita una asignacion docente, una defensa personal ni los ochenta minutos del examen. Todos los commits corresponden a trabajo real de esta sesion.

| Cambio | Donde explicarlo | Que demostrar |
|---|---|---|
| SC-A | shared/.../domain/usecase/FiltrarRutaUseCase.kt y presentation/listado/EcoViewModel.kt | Buscar Empresa 6, activar Solo pendientes de pesar y repetir en modo avion. Al registrar localmente, desaparece del filtro por el Flow de SQLite. |
| SC-B | Backend seguridad/Usuario.kt, Semillas.kt; cliente Sesion.soloLectura y AuditorClienteTest | Entrar con auditor1; leer padron y detalle, sin botones de escritura. DELETE directo devuelve 403 con sesion vigente. |
| SC-C | Backend PesajePeticion, ReglasPesaje, EcoRepository y schema.sql; cliente Contratos.kt, BaseLocal.kt, Eco.sq y 2.sqm | Rechazar 101; guardar 18.25 offline; sincronizar y mostrar el mismo valor con ID definitivo en PostgreSQL. |
| SC-D | cantidadPendientes en Eco.sq; BaseLocal.pendientes; ObservarPendientesUseCase; UiState.pendientesSync | Mostrar 0; registrar sin red y ver 1; reabrir sin red y conservar 1; sincronizar y ver 0. |

Las rutas abreviadas de shared corresponden a `shared/src/commonMain/kotlin/pe/upeu/ecorecicla`; SQL esta en `shared/src/commonMain/sqldelight/pe/upeu/ecorecicla/db`. Compose solo presenta el estado y emite eventos, sin filtrar listas ni calcular el contador.

## Decisiones que conviene defender

El PDF no define unidad ni precision de humedad: se adopta porcentaje opcional 0..100 con hasta dos decimales. Null significa sin estimar y conserva compatibilidad con registros anteriores. El servicio valida el valor antes de persistirlo; la app lo valida para dar respuesta inmediata. No se altera la formula del peso neto ni se inventa un descuento por humedad. La migracion SQLite 2→3 preserva IDs negativos, claves de idempotencia y pendientes. PostgreSQL agrega la columna sin borrar datos y actualiza solo filas identificadas como semillas.

«Pendiente de pesar» y «pendiente de sincronizar» son conceptos diferentes. El primero corresponde a una visita sin pesaje valido ni omision justificada, dentro de la ruta seleccionada. El segundo cuenta filas con `pendiente=1` en todo el dispositivo: creaciones, correcciones y anulaciones. Por eso el buscador de SC-A no modifica el distintivo de SC-D. Una operacion rechazada sigue contando hasta corregirse, sincronizarse o descartarse expresamente.

AUDITOR no requiere condiciones por nombre de rol. La matriz del backend le asigna PUNTO_LEER y PESAJE_LEER. La pantalla inicial depende de la ausencia de PESAJE_CREAR y abre el padron; las acciones consultan privilegios. Ocultar botones no protege el API: los metodos del servidor repiten la comprobacion con authorities del JWT y las pruebas llaman directamente a las escrituras prohibidas.

## Accesos y vistas

Clave comun de demostracion: `Eco2026*`.

| Usuarios | Vistas y privilegios |
|---|---|
| operario1, operario2 | Mi ruta, Puntos, detalle, formulario y Perfil. Lee, registra y corrige propios del mismo dia. No anula, cierra ni administra. |
| supervisor1 | Ademas Rutas de operaciones, seleccion de ruta, correccion global, anulacion logica, omisiones y cierre validado. No administra cuentas. |
| admin | Padron, detalle de lectura, Rutas, Administrar y Perfil. Gestiona puntos, usuarios, roles y asignaciones. No registra, corrige ni anula pesajes ni cierra rutas. |
| auditor1 | Padron, detalle de lectura y Perfil; indicador Acceso de consulta. Solo PUNTO_LEER y PESAJE_LEER. |

El menu Mi ruta sigue disponible para los lectores aunque no tengan ruta asignada; muestra el estado vacio. La ruta QA #4 de operario2 fue cerrada durante la verificacion; no se finge que sea una ruta semilla abierta. La ruta original de operario1 sigue abierta con puntos pendientes para demostrar RN04 y SC-A.

## Pruebas y evidencia real

Backend: `gradlew.bat build` pasa 35 casos Spring. `node scripts/verificar-api.mjs` verifica 35 peticiones contra PostgreSQL y exporta las respuestas observadas en la coleccion Postman, sin tokens validos. Incluye 400 por humedad, 401 y 403 del auditor.

Android: `gradlew.bat :androidApp:assembleDebug :shared:allTests` pasa. Son 31 casos comunes, ejecutados en JVM y variantes unitarias Android, mas dos migraciones JVM (33 casos en el informe JVM). No se suman las variantes como pruebas distintas. FiltroRutaTest cubre combinacion y actualizacion sin red; AuditorClienteTest bloquea escrituras antes de encolarlas; HumedadClienteTest comprueba almacenamiento y cuerpo HTTP; ContadorPendientesTest recibe emisiones de una consulta real SQLite.

Las capturas de `evidencias/parte2` muestran ejecucion real del emulador. Se verificaron en PostgreSQL los pesajes QA #25 y #26 con humedades 18.25 y 7.50. El PDF actualizado incluye las pantallas, un rechazo 403 real, resultados XML y graficos del historial obtenido con Git. Los TXT contiguos conservan las lineas completas del grafico.

## Ramas y avances reales

| Rama | Commits de implementacion, prueba y presentacion |
|---|---|
| Android sc-a-qquerar | 3b0768f, 989f1be, f8264a1; PR #15 |
| Android sc-b-qquerar | cca028f, 4d11dc9, e100896; PR #16 |
| Android sc-c-qquerar | 2143226, 76e7fcb, 0933e60; PR #17 |
| Android sc-d-qquerar | 46c5e65, 15bb505, deb9979, 6248737; PR #18 |
| Backend sc-b-qquerar | 14482c1, 6c51a6f, 0dade71; PR #15 |
| Backend sc-c-qquerar | 1de8607, 1a70264, 6aeafae; PR #16 |

Cada rama parte de develop y vuelve por PR con merge commit. Main recibe la integracion desde develop. No se fuerza la etiqueta anterior `v1.0-unidad2`; la entrega actual se identifica con `v1.2.1-unidad2`. El historial no satisface por si mismo el criterio academico de trabajo distribuido en varias fechas de la unidad.
