# Inkr8 — pruebas del capítulo 10

Revisión y entrega del grupo con asistencia de IA declarada. La participación y reproducción individual no se presume.

JDK21, Gradle9.1.0, AndroidSDK36. Ejecutar `:app:testDebugUnitTest --rerun :domain:test --rerun --continue`; son96casos y la base conserva3fallos de contrato de mensaje. La corrección mínima de producto se entrega aparte. Los diez casos ClienteNegativeFlowTest sustituyen tres comparaciones de constantes por fallos/datos inducidos y ViewModels/mapper reales.

`:app:assembleDebug :app:assembleDebugAndroidTest` prepara instrumentadas. Capitulo10VisualTest prueba6casos con Compose real y repositorios controlados; Capitulo10LocalFirestoreTest prueba2casos con SDK Android real y reglas productivas en emulador demo-inkr8 (10.0.2.2:8080), con inicio de perfil/sesión controlado. No se usa Google/OpenAI real.

La aplicación sin google-services.json muestra la pantalla de configuración existente. Los tests de componentes no sustituyen el inicio de sesión real ni una integración cloud. Ver README_Ejecucion.md del expediente para instalación local, capturas auténticas y parches separados.
