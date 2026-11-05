# Constitución de la refactorización de Inkr8

## Alcance obligatorio
- Mantener Kotlin/Compose en Android y TypeScript/Firebase Functions en backend.
- Únicamente cinco destinos de navegación: Login, Home con palabra del día, Practice, Ranked y Profile. Escritura, espera y resultado pertenecen al mismo destino de juego.
- Conservar temporadas, práctica, competitivo, ganancia básica de Merit y entrada Ranked. El costo fijo inicial será 100; saldo inicial 1000. Sin transferencias, impuestos, retenciones, membresías ni bonificaciones por rachas.
- Purgar torneos, reputación, publicidad, pantallas ajenas al alcance y nombres conceptuales de ligas. Las ligas son números 1–6. La ampliación del 17 de octubre de 2026 reincorpora las estadísticas e historial competitivos en Perfil para atender las historias de usuario; Practice queda excluida de esas estadísticas.
- El servidor determina períodos UTC de exactamente 14 días desde 2026-10-12T00:00:00Z. Antes del ancla se aplica también la misma fórmula de períodos. Reiniciar rating a cero en cada frontera; Merit no se reinicia. El proceso debe ser idempotente, paginado y seguro frente a partidas concurrentes.

## Arquitectura y SOLID
- Dependencias dirigidas hacia Domain: Presentation → Domain ← Data. Composition root conecta implementaciones sin exponer Firebase a UI.
- Domain Android será un módulo JVM Kotlin independiente: entidades, reglas, casos de uso e interfaces pequeñas. Prohibidos Android, Compose, Firebase y DTOs de infraestructura.
- Data contiene adaptadores Firebase, fuentes de datos, DTOs y mappers; no reglas de negocio ni composables.
- Presentation contiene ViewModels, StateFlow/UI State y Compose; no accesos directos a base de datos ni cálculos de recompensas/rating.
- Backend sigue Domain, Data, Presentation (handlers HTTP y scheduler) y composition root; Domain no importa Firebase/OpenAI.
- SRP: cada componente tiene una sola responsabilidad. OCP: ampliar mediante contratos. LSP: adaptadores respetan contratos y errores. ISP: contratos por capacidad. DIP: casos de uso dependen de interfaces e inyección por constructor.
- Evitar singletons de negocio, dependencias por defecto ocultas, clases de estado global y repositorios monolíticos.
- El ciclo de borradores de Presentation pasa por casos de uso del dominio. Compose consume contador y validación publicados en UI State; no ejecuta esas políticas.
- Composition root permite reemplazar repositorios por sus contratos e inicializa adaptadores predeterminados sólo cuando se necesitan.

## Kotlin y errores
- Tipos explícitos en contratos, propiedades inmutables, nombres PascalCase/camelCase, paquetes minúsculos por capa y funcionalidad.
- Usar suspend/Flow, cancelar observers al cambiar sesión y liberar listeners. Nunca absorber CancellationException.
- Estados de carga/éxito/error explícitos; errores útiles y reintento visible. No inventar éxitos, balances, evaluaciones ni configuración de producción.
- Validar escritura tanto en dominio cliente como servidor; Firebase Functions es autoridad del usuario, Merit, rating y temporada.
- Operaciones económicas atómicas e idempotentes por ID de partida. Rating y recompensa confirmados junto con el resultado. Nunca confiar en authorId, costo, score, timestamp o temporada enviados por cliente.
- Autorización Firestore de lectura por propietario; escritura económica sólo por backend. Secretos fuera del repositorio.

## Restricciones y validación
- Crear estos artefactos antes de modificar fuentes. Ejecutar slices en orden, máximo diez.
- Carpeta externa `../refactoring_prompts` (respecto de la raíz del proyecto): únicamente prompts del proceso, sin código, informes o resultados.
- No desplegar ni modificar datos remotos durante la refactorización. Documentar configuración Firebase ausente sin inventar credenciales.
- Preservar recursos de identidad y datos remotos existentes; eliminar únicamente código local fuera de alcance. Campos retirados no se usan en nuevos DTOs.
- Validar invariantes económicas, fronteras de temporada, aislamiento de capas, autenticación y concurrencia. Ejecutar build/test TypeScript y assemble/test/lint Android. Registrar evidencia real y límites en informe final.
- Las correcciones de arquitectura y SOLID no agregan dependencias externas en Gradle ni package.json.
