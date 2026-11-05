# Inkr8 · Android Frontend (Inkr8Android)

Cliente móvil nativo para **Inkr8**, una plataforma de práctica y escritura competitiva en inglés evaluada por inteligencia artificial (R8).

## Arquitectura del Proyecto

El frontend está desarrollado bajo los principios de **Clean Architecture** y separación estricta de responsabilidades en Kotlin:

```
Inkr8Android/
├── app/                  # Módulo Android (Presentation y Data)
│   ├── presentation/     # UI en Jetpack Compose, ViewModels (MVI/StateFlow)
│   │   ├── ui/           # Pantallas: HomeScreen, GameScreen, ProfileScreen, LoginScreen, AcceptanceShowcaseDialog
│   │   ├── viewmodel/    # HomeViewModel, GameViewModel, ProfileViewModel, SessionViewModel, RankingViewModel
│   │   └── state/        # UiStates y AcceptanceMessages
│   ├── data/             # Implementación de datos (DataSources, Repositorios cliente, DTOs, Mappers)
│   └── di/               # Composition Root manual (AppGraph.kt, Inkr8Root.kt)
├── domain/               # Módulo Kotlin JVM Puro (sin dependencias de Android ni Firebase)
│   ├── model/            # Entidades (User, Game, Home, Word, Evaluation, Season, etc.)
│   ├── policy/           # Políticas de negocio (GamePolicy, AcceptanceMessages)
│   ├── repository/       # Contratos de repositorios (UserRepository, GameRepository, etc.)
│   └── usecase/          # Casos de uso de autenticación, contenido, juego, borradores y perfil
└── scripts/              # Verificación automática de fronteras arquitectónicas
```

---

## Criterios de Aceptación y Pruebas

El proyecto implementa al 100% los **60 criterios de aceptación** (éxito y fallo para 30 historias de usuario) derivados de `ASR_Inkr8.xlsx`.

- **Demostración en vivo en la App**:
  En la barra superior de la app se encuentra el botón **`Criterios AC`**, el cual abre un panel interactivo (`AcceptanceShowcaseDialog`) para simular y proyectar cualquier criterio canónico en ejecución.
- **Suite de Pruebas Automatizadas**:
  El archivo `app/src/test/java/com/inkr8/presentation/AcceptanceCriteriaSuiteTest.kt` contiene los 60 tests unitarios independientes cubriendo todas las historias (`hu1_1_...` a `hu3_15_...`).

### Ejecución de Pruebas
```powershell
./gradlew testDebugUnitTest --tests "com.inkr8.presentation.AcceptanceCriteriaSuiteTest"
```

### Verificación de Arquitectura
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-architecture.ps1
```

---

## Integración con el Backend (Inkr8BackEnd)

La aplicación se comunica con el backend de Firebase mediante:
1. **Firebase Authentication**: Inicio de sesión persistente con Google OAuth.
2. **Cloud Functions (HTTPS Callables)**:
   - `initializeUser`, `updateProfile`
   - `getHome`, `getWordExample`
   - `startGame`, `submitGame`, `retryEvaluation`
   - `getProfileProgress`, `getSeasonHistory`, `getSeasonRanking`
3. **Cloud Firestore (Tiempo Real)**:
   - Observadores de cambios en partidas (`submissions/{gameId}`) para recibir notificaciones de evaluación completada y emparejamientos en tiempo real.
   - Observadores del perfil de usuario (`users/{userId}`).

Para conectar con el entorno Firebase de producción o emuladores locales, coloque el archivo `google-services.json` correspondiente en la carpeta `app/`.
