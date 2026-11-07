package com.inkr8.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.inkr8.data.dto.GameDto
import com.inkr8.data.mapper.toDomain
import com.inkr8.di.AppGraph
import com.inkr8.di.Inkr8Root
import com.inkr8.domain.model.*
import com.inkr8.domain.repository.*
import com.inkr8.domain.usecase.*
import com.inkr8.presentation.state.AcceptanceMessages
import com.inkr8.presentation.ui.GameScreen
import com.inkr8.presentation.ui.Inkr8Theme
import com.inkr8.presentation.viewmodel.*
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose and ViewModels on an Android emulator. Repositories are explicit doubles.
 * Group review with AI assistance; this is not a Google/OpenAI/cloud acceptance run. */
@RunWith(AndroidJUnit4::class)
class Capitulo10VisualTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    @After fun clear() { compose.runOnUiThread { store.clear() } }

    @Test fun UI_HU36_C01_profileReadFailureRendersRequiredMessage() {
        val users = VisualUsers().apply { failObserve = true }
        val auth = VisualAuth()
        lateinit var session: SessionViewModel
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val graph = AppGraph(context, auth, users, VisualGames(), VisualHome(), VisualSeasons(), VisualDrafts(), progressRepository = VisualProgress())
        compose.runOnUiThread {
            session = SessionViewModel(ObserveSession(auth), InitializeUser(users), ObserveUser(users), SignIn(auth), SignOut(auth))
            store.put("session", session)
        }
        compose.setContent { Inkr8Root(graph, session, {}) }
        compose.waitUntil(10_000) { session.state.value.error != null }
        compose.onNodeWithText("Perfil", useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) { users.observeCalls >= 2 && session.state.value.error != null }
        compose.waitForIdle()
        capture("UI-HU36-C01")
        assertEquals(145L, session.state.value.user?.rating)
        assertFalse(session.state.value.loading)
        assertTrue(session.state.value.signedIn)
        assertEquals(0, users.writes)
        assertEquals(0, auth.signOutCalls)
        compose.onNodeWithText(AcceptanceMessages.SERVER_FAILURE).assertIsDisplayed()
    }

    @Test fun UI_HU38_C01_resultObservationFailureRendersRequiredMessage() {
        val games = VisualGames().apply { failObserve = true }
        val vm = renderGame(games)
        compose.waitUntil(10_000) { vm.state.value.error != null }
        capture("UI-HU38-C01")
        assertEquals("visual-result", vm.state.value.game?.id)
        assertFalse(vm.state.value.busy)
        assertEquals(0, games.writes)
        compose.onNodeWithText(AcceptanceMessages.GAME_RESULT_UPDATE_FAILED).assertIsDisplayed()
    }

    @Test fun UI_HU38_C02_invalidResultPayloadRendersFailureThroughMapperAndViewModel() {
        val games = VisualGames().apply { outcome = "UNRECOGNIZED" }
        val vm = renderGame(games)
        compose.waitUntil(10_000) { games.observations > 0 }
        assertNull(vm.state.value.game?.match)
        assertEquals(MatchStatus.MATCHED, vm.state.value.game?.matchStatus)
        compose.onNodeWithText(AcceptanceMessages.GAME_RESULT_UPDATE_FAILED).performScrollTo().assertIsDisplayed()
        capture("UI-HU38-C02")
        assertEquals(0, games.writes)
    }

    @Test fun UI_HU39_C01_invalidRatingPayloadRendersServerFailure() {
        val games = VisualGames().apply { delta = "unavailable" }
        val vm = renderGame(games)
        compose.waitUntil(10_000) { games.observations > 0 }
        assertNotNull(vm.state.value.game?.match)
        assertNull(vm.state.value.game?.match?.ratingChange)
        compose.onNodeWithText(AcceptanceMessages.SERVER_FAILURE).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rating +0").assertDoesNotExist()
        capture("UI-HU39-C01")
        assertEquals(0, games.writes)
    }

    @Test fun UI_HU38_C03_failureWhileWaitingForSubmittedResultRendersRequiredMessage() {
        val games = VisualGames().apply { status = GameStatus.PENDING; failObserve = true }
        val vm = renderGame(games)
        compose.waitUntil(10_000) { vm.state.value.error != null }
        capture("UI-HU38-C03")
        assertEquals(GameStatus.PENDING, vm.state.value.game?.status)
        assertFalse(vm.state.value.busy)
        assertEquals(0, games.writes)
        compose.onNodeWithText(AcceptanceMessages.GAME_RESULT_UPDATE_FAILED).assertIsDisplayed()
    }

    @Test fun UI_HU39_C02_reconnectRestoresRatingWithoutAnotherSubmission() {
        val games = VisualGames().apply { delta = "unavailable" }
        val vm = renderGame(games)
        compose.onNodeWithText(AcceptanceMessages.SERVER_FAILURE).performScrollTo().assertIsDisplayed()
        capture("UI-HU39-C02-before")
        compose.runOnUiThread { games.delta = -6L; vm.reconnect() }
        compose.waitUntil(10_000) { vm.state.value.game?.match?.ratingChange == -6L }
        compose.onNodeWithText("Rating -6").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(AcceptanceMessages.SERVER_FAILURE).assertDoesNotExist()
        capture("UI-HU39-C02-after")
        assertEquals(2, games.observations)
        assertEquals(0, games.writes)
    }

    private fun renderGame(games: VisualGames): GameViewModel {
        val drafts = VisualDrafts()
        lateinit var vm: GameViewModel
        compose.runOnUiThread {
            vm = GameViewModel("visual-user", PlayMode.RANKED, StartGame(games), SubmitWriting(games), ObserveGame(games),
                GetRecentGames(games), RetryEvaluation(games), LoadDraft(drafts), SaveDraft(drafts), ClearDraft(drafts), FindDraft(drafts))
            store.put("game", vm)
            vm.open(games.game())
        }
        compose.setContent { Inkr8Theme { Surface(Modifier.fillMaxSize()) { GameScreen(vm, PlayMode.RANKED) } } }
        compose.waitForIdle()
        return vm
    }

    private fun capture(id: String) {
        compose.waitForIdle()
        // PixelCopy waits for a rendered frame before the device screenshot.
        // The fixture uses the same themed surface as the real app Scaffold.
        compose.onRoot().captureToImage()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.filesDir, "capitulo10-evidencias").apply { mkdirs() }
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(folder, "$id.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        File(folder, "$id.txt").writeText("case=$id\nrepositories=controlled doubles\nui=real Compose + ViewModel\ncloud=false\n" + compose.onRoot().printToString())
    }
}

internal class VisualAuth : AuthRepository {
    private val session = MutableStateFlow<String?>("visual-user")
    var signOutCalls = 0
    override fun observeSession(): Flow<String?> = session
    override suspend fun signInWithGoogle(token: String) { error("Google must not be called by this test") }
    override suspend fun signOut() { signOutCalls++; session.value = null }
}
internal class VisualUsers : UserRepository {
    var failObserve = false
    @Volatile var observeCalls = 0
    var writes = 0
    val user = User("visual-user", "Inkr8 Test", "visual@example.test", 1000, 145, 4, "visual")
    override suspend fun initialize() = user
    override fun observeUser(id: String): Flow<User> = flow {
        observeCalls++; emit(user)
        if (failObserve) throw IOException("injected-profile-read")
    }
    override suspend fun updateName(name: String) { writes++; error("Unexpected mutation") }
}
internal class VisualGames : GameRepository {
    var failObserve = false
    var status = GameStatus.EVALUATED
    var outcome = "WIN"
    var delta: Any = 4L
    @Volatile var observations = 0
    var writes = 0
    fun game() = GameDto("visual-result", mapOf(
        "mode" to "RANKED", "writingMode" to "STANDARD", "status" to status.name, "matchStatus" to if (status == GameStatus.EVALUATED) "MATCHED" else "NONE",
        "authorId" to "visual-user", "seasonIndex" to 0, "timestamp" to 1791763200000L,
        "challenge" to mapOf("words" to listOf("ink", "tree", "life", "word").map {
            mapOf("text" to it, "definition" to "Definition", "sentence" to "Use $it.", "phonetic" to "/$it/", "partOfSpeech" to "noun", "rarity" to "Common")
        }), "content" to List(50) { "ink" }.joinToString(" "),
        "evaluation" to if (status == GameStatus.EVALUATED) mapOf("score" to 82.0, "feedback" to "Controlled response", "meritEarned" to 195L,
            "spelling" to "Correct", "vocabulary" to "Varied", "coherence" to "Clear", "creativity" to "Good", "constraints" to emptyList<Any>()) else null,
        "match" to if (status == GameStatus.EVALUATED) mapOf("opponentName" to "Rival", "opponentScore" to 60.0, "outcome" to outcome, "ratingChange" to delta) else null
    )).toDomain()
    override fun observe(id: String): Flow<Game> = flow {
        observations++; emit(game())
        if (failObserve) throw IOException("injected-game-read")
    }
    override suspend fun recent(): List<Game> = emptyList()
    override suspend fun start(id: String, mode: PlayMode, writingMode: WritingMode): Game { writes++; error("Unexpected start") }
    override suspend fun submit(id: String, content: String) { writes++; error("Unexpected submit") }
    override suspend fun retryEvaluation(id: String) { writes++; error("Unexpected retry") }
}
internal class VisualHome : HomeRepository {
    override suspend fun getHome() = Home(Word("ink", "Writing fluid", "I use ink.", "/ink/", "noun", "Common"), Season("s0", 1791763200000, 1794355200000), 100, 1)
    override suspend fun getExample(day: Long) = "I use ink."
}
internal class VisualSeasons : SeasonRepository {
    override suspend fun getRanking() = SeasonRanking(Season("s0", 1791763200000, 1794355200000), emptyList())
    override suspend fun getHistory(): List<SeasonHistory> = emptyList()
}
internal class VisualProgress : ProgressRepository {
    override suspend fun getProgress() = Progress(emptyList(), Statistics(0, 0.0, 0.0, 0.0))
}
internal class VisualDrafts : DraftRepository {
    override fun load(userId: String, mode: PlayMode): Draft? = null
    override fun find(userId: String, gameId: String): Draft? = null
    override fun save(userId: String, mode: PlayMode, draft: Draft) {}
    override fun clear(userId: String, mode: PlayMode) {}
}
