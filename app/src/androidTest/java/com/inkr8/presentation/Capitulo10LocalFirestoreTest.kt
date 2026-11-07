package com.inkr8.presentation

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import com.inkr8.data.repository.FirebaseUserRepository
import com.inkr8.data.source.FirestoreDataSource
import com.inkr8.data.source.FunctionsDataSource
import com.inkr8.di.AppGraph
import com.inkr8.di.Inkr8Root
import com.inkr8.domain.model.User
import com.inkr8.domain.repository.UserRepository
import com.inkr8.domain.usecase.*
import com.inkr8.presentation.state.AcceptanceMessages
import com.inkr8.presentation.viewmodel.SessionViewModel
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android Firestore SDK -> loopback emulator -> production security rules.
 * No Google account, remote Firebase project or OpenAI request is used. */
@RunWith(AndroidJUnit4::class)
class Capitulo10LocalFirestoreTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private var app: FirebaseApp? = null
    private var firestore: FirebaseFirestore? = null
    @After fun clear() {
        compose.runOnUiThread { store.clear() }
        firestore?.terminate()
        app?.delete()
    }

    private fun realRepository(): FirebaseUserRepository {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        check(args.getString("localFirestore") == "true") { "Start the demo-inkr8 emulator, then opt in with -e localFirestore true" }
        val options = FirebaseOptions.Builder().setProjectId("demo-inkr8")
            .setApplicationId("1:1234567890:android:0000000000000000000000")
            .setApiKey("fake-local-emulator-only").build()
        val localApp = FirebaseApp.initializeApp(instrumentation.targetContext, options, "cap10-" + UUID.randomUUID())
        localApp.setDataCollectionDefaultEnabled(false)
        app = localApp
        val db = FirebaseFirestore.getInstance(localApp)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        db.useEmulator("10.0.2.2", 8080)
        firestore = db
        val functions = FirebaseFunctions.getInstance(localApp, "us-central1")
        functions.useEmulator("10.0.2.2", 5001)
        return FirebaseUserRepository(FunctionsDataSource(functions), FirestoreDataSource(db))
    }

    @Test fun INT_HU36_C01_unauthenticatedAndroidSdkReadIsDeniedByRealRules() = runBlocking {
        val repository = realRepository()
        var captured: Throwable? = null
        try { withTimeout(20_000) { repository.observeUser("visual-user").first() } }
        catch (error: FirebaseFirestoreException) { captured = error }
        assertTrue("A real Firestore exception must reach the real repository", captured is FirebaseFirestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, (captured as FirebaseFirestoreException).code)
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "capitulo10-evidencias").apply { mkdirs() }
        File(folder, "INT-HU36-C01.txt").writeText("project=demo-inkr8\nhost=10.0.2.2:8080\nsdk=real Android Firestore\nrules=production file\nidentity=none\nresult=PERMISSION_DENIED\nerror=" + captured.toString())
    }

    @Test fun INT_HU36_C02_realSdkReadFailureTraversesViewModelAndScreen() {
        val repository = realRepository()
        val values = VisualUsers()
        var actualCode: FirebaseFirestoreException.Code? = null
        val users = object : UserRepository {
            override suspend fun initialize(): User = values.user // Only initialization is a controlled fixture.
            override fun observeUser(id: String): Flow<User> = repository.observeUser(id).catch { error ->
                actualCode = (error as? FirebaseFirestoreException)?.code
                throw error
            }
            override suspend fun updateName(name: String) { error("No write permitted in this test") }
        }
        val auth = VisualAuth()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val graph = AppGraph(context, auth, users, VisualGames(), VisualHome(), VisualSeasons(), VisualDrafts(), progressRepository = VisualProgress())
        lateinit var session: SessionViewModel
        compose.runOnUiThread {
            session = SessionViewModel(ObserveSession(auth), InitializeUser(users), ObserveUser(users), SignIn(auth), SignOut(auth))
            store.put("session", session)
        }
        compose.setContent { Inkr8Root(graph, session, {}) }
        compose.waitUntil(25_000) { session.state.value.error != null }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, actualCode)
        compose.waitForIdle()
        compose.onRoot().captureToImage()
        val folder = File(context.filesDir, "capitulo10-evidencias").apply { mkdirs() }
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(folder, "INT-HU36-C02.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        File(folder, "INT-HU36-C02.txt").writeText("sdk=real Firestore emulator\ninitialize=controlled fixture\nauthSession=controlled fixture\nerrorCode=$actualCode\nstateError=" + session.state.value.error + "\n" + compose.onRoot().printToString())
        compose.onNodeWithText(AcceptanceMessages.SERVER_FAILURE).assertIsDisplayed()
    }
}
