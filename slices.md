# Plan secuencial de Inkr8

Los estados y la evidencia se registran al completar cada etapa. No saltar etapas.

| Slice | Objetivo | Paquetes/archivos afectados | Aceptación / validación |
|---|---|---|---|
| 1 | Inventario, contratos y herramientas reproducibles | raíz Gradle, catálogo, wrapper, package.json, documentación | Lenguajes conservados; configuración coherente; inventario y límites documentados |
| 2 | Dominio puro y políticas de juego | domain/src, functions/src/domain | Entidades y contratos independientes; reglas de Merit, ligas y períodos de 14 días verificadas |
| 3 | Backend por capas y economía atómica | functions/src/data, presentation, index.ts | Usuario autenticado; débito único por ID; recompensa única; evaluación real; acceso protegido |
| 4 | Reinicio global de rating y temporadas | backend domain/data/presentation de temporadas | Scheduler paginado idempotente; normalización en partidas; frontera UTC probada; no borrar Merit |
| 5 | Adaptadores Android e inyección | app/data, app/di, manifest y configuración Firebase | DTOs/mappers y fuentes aisladas; streams cancelables; contratos integrados |
| 6 | Cinco pantallas y estados MVVM | app/presentation, Activity/Application | Login, Home, Practice, Ranked, Profile; escritura y resultado integrados; errores y reintentos visibles |
| 7 | Purga integral y seguridad | fuentes antiguas, recursos, dependencias, Firestore rules | Sin sistemas excluidos ni ligas conceptuales; reglas protegen economía y datos por usuario |
| 8 | Validación y entrega | tests, builds, README, REFACTORING_REPORT.md | Backend build/tests; dominio tests; Android assemble/lint; evidencia y configuración pendiente explícitas |

## Estado
- Artefactos iniciales creados antes de cambios de código.
- Slices 1–8: completados en orden en el código local.
- Slice 8: 24 pruebas aprobadas (8 TypeScript, 9 Firestore, 3 dominio Kotlin y 4 ViewModels). `assembleDebug`, `assembleRelease`, `testDebugUnitTest`, `domain:test` y `lintDebug` aprobados. Lint: `No issues found.` Arquitectura: PASS. Informe y README entregados.
- Configuración externa pendiente para operación remota: `app/google-services.json`, OAuth Google, secreto OpenAI y despliegue Firebase. No se han inventado credenciales ni modificado servicios remotos.
