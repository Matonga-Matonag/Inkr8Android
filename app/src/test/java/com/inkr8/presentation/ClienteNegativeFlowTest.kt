package com.inkr8.presentation

import androidx.lifecycle.ViewModelStore
import com.inkr8.data.dto.GameDto
import com.inkr8.data.mapper.toDomain
import com.inkr8.domain.model.*
import com.inkr8.domain.repository.*
import com.inkr8.domain.usecase.*
import com.inkr8.presentation.viewmodel.GameViewModel
import com.inkr8.presentation.viewmodel.SessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** Real ViewModels/use cases/mapping; only repository I/O is controlled. No screen is rendered here. */
@OptIn(ExperimentalCoroutinesApi::class)
class ClienteNegativeFlowTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun CLI_HU36_C01_profileReadFailurePreservesSessionAndReportsServerFailure() = runTest(dispatcher) {
        val users = FailingUsers().apply { failObserve = true }
        val auth = ControlledAuth()
        val vm = session(users, auth); runCurrent()
        assertEquals(1, users.initializeCalls); assertEquals(1, users.observeCalls)
        assertTrue(vm.state.value.signedIn); assertFalse(vm.state.value.loading)
        assertEquals(users.current, vm.state.value.user)
        assertEquals(145L, vm.state.value.user?.rating)
        assertEquals(0, users.updateCalls); assertEquals(0, auth.signOutCalls)
        assertEquals("Server failure", vm.state.value.error)
    }

    @Test fun CLI_HU36_C02_profileReadCanRecoverWithoutLogoutOrWrites() = runTest(dispatcher) {
        val users = FailingUsers().apply { failObserve = true }
        val auth = ControlledAuth()
        val vm = session(users, auth); runCurrent()
        assertNotNull(vm.state.value.error)
        users.failObserve = false; users.current = users.current.copy(rating = 149)
        vm.refresh(); runCurrent()
        assertEquals(2, users.observeCalls); assertEquals(149L, vm.state.value.user?.rating)
        assertNull(vm.state.value.error); assertFalse(vm.state.value.loading)
        assertTrue(vm.state.value.signedIn)
        assertEquals(0, users.updateCalls); assertEquals(0, auth.signOutCalls)
    }

    @Test fun CLI_HU36_C03_initializationFailureRetainsTheSeparateProgressMessage() = runTest(dispatcher) {
        val users = FailingUsers().apply { failInitialize = true }
        val vm = session(users, ControlledAuth()); runCurrent()
        assertEquals(1, users.initializeCalls); assertEquals(0, users.observeCalls)
        assertTrue(vm.state.value.signedIn); assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.user)
        assertEquals("We were unable to retrieve your progress information. Please try again", vm.state.value.error)
        assertEquals(0, users.updateCalls)
    }

    @Test fun CLI_HU38_C01_resultObservationFailureUsesCanonicalMessageWithoutReplayingGame() = runTest(dispatcher) {
        val games = FailingGames().apply { failObserve = true }
        val vm = game(games); runCurrent(); vm.open(games.mapped()); runCurrent()
        assertEquals(1, games.observeCalls); assertEquals(0, games.activeObservers)
        assertEquals(82.0, vm.state.value.game!!.evaluation!!.score, 0.0)
        assertEquals("WIN", vm.state.value.game?.match?.outcome)
        assertFalse(vm.state.value.busy); assertFalse(vm.state.value.canSubmit)
        assertNoGameMutation(games)
        assertEquals("The game result could not be updated", vm.state.value.error)
    }

    @Test fun CLI_HU38_C02_resultObservationCanReconnectAndClearTheFailure() = runTest(dispatcher) {
        val games = FailingGames().apply { failObserve = true }
        val vm = game(games); runCurrent(); vm.open(games.mapped()); runCurrent()
        assertNotNull(vm.state.value.error)
        games.failObserve = false
        vm.reconnect(); runCurrent()
        assertEquals(2, games.observeCalls); assertEquals(0, games.activeObservers)
        assertNull(vm.state.value.error)
        assertEquals("ranked-result", vm.state.value.game?.id)
        assertEquals("WIN", vm.state.value.game?.match?.outcome)
        assertNoGameMutation(games)
    }

    @Test fun CLI_HU38_C03_malformedResultIsRejectedThroughMapperAndRealObserver() = runTest(dispatcher) {
        val games = FailingGames().apply { matchFields = matchFields + ("outcome" to "UNRECOGNIZED") }
        val vm = game(games); runCurrent(); vm.open(games.mapped()); runCurrent()
        assertEquals(1, games.observeCalls)
        assertEquals(MatchStatus.MATCHED, vm.state.value.game?.matchStatus)
        assertNull(vm.state.value.game?.match)
        assertEquals(82.0, vm.state.value.game!!.evaluation!!.score, 0.0)
        assertEquals(195L, vm.state.value.game?.evaluation?.meritEarned)
        assertNoGameMutation(games)
        // GameScreen renders GAME_RESULT_UPDATE_FAILED from MATCHED + match == null.
        // The separate instrumented test must verify that visible message.
    }

    @Test fun CLI_HU38_C04_failureWhileAwaitingSubmittedResultUsesCanonicalMessage() = runTest(dispatcher) {
        val games = FailingGames().apply { gameStatus = GameStatus.PENDING; failObserve = true }
        val vm = game(games); runCurrent(); vm.open(games.mapped()); runCurrent()
        assertEquals(1, games.observeCalls); assertEquals(0, games.activeObservers)
        assertEquals(GameStatus.PENDING, vm.state.value.game?.status)
        assertEquals(50, vm.state.value.wordCount)
        assertNull(vm.state.value.game?.evaluation); assertNull(vm.state.value.game?.match)
        assertFalse(vm.state.value.busy); assertFalse(vm.state.value.canSubmit)
        assertNoGameMutation(games)
        assertEquals("The game result could not be updated", vm.state.value.error)
    }

    @Test fun CLI_HU38_C05_draftObservationRetainsItsSeparateNetworkFailure() = runTest(dispatcher) {
        val games = FailingGames().apply { gameStatus = GameStatus.DRAFT; failObserve = true }
        val vm = game(games); runCurrent(); vm.open(games.mapped()); runCurrent()
        assertEquals(GameStatus.DRAFT, vm.state.value.game?.status)
        assertEquals("", vm.state.value.text)
        assertFalse(vm.state.value.busy); assertFalse(vm.state.value.canSubmit)
        assertEquals("injected-game-read", vm.state.value.error)
        assertNoGameMutation(games)
    }

    @Test fun CLI_HU39_C01_failedRatingVariationIsNotInventedAsZeroAndKeepsTheResult() = runTest(dispatcher) {
        val games = FailingGames().apply { matchFields = matchFields + ("ratingChange" to "unavailable") }
        val vm = game(games); runCurrent(); vm.open(games.mapped()); runCurrent()
        assertEquals(1, games.observeCalls); assertEquals(0, games.activeObservers)
        val result = vm.state.value.game!!
        assertEquals(MatchStatus.MATCHED, result.matchStatus)
        assertEquals("WIN", result.match?.outcome); assertEquals("Rival", result.match?.opponentName)
        assertNull(result.match?.ratingChange)
        assertEquals(82.0, result.evaluation!!.score, 0.0)
        assertEquals(195L, result.evaluation!!.meritEarned)
        assertFalse(vm.state.value.busy); assertFalse(vm.state.value.canSubmit)
        assertNoGameMutation(games)
        // GameScreen renders SERVER_FAILURE for a real match with a null ratingChange.
        // This is state evidence, not a constant assertion or a claim of UI execution.
    }

    @Test fun CLI_HU39_C02_reconnectRestoresTheRealVariationWithoutNewSubmission() = runTest(dispatcher) {
        val games = FailingGames().apply { matchFields = matchFields + ("ratingChange" to "unavailable") }
        val vm = game(games); runCurrent(); vm.open(games.mapped()); runCurrent()
        assertNull(vm.state.value.game?.match?.ratingChange)
        games.matchFields = games.matchFields + ("ratingChange" to -6L)
        vm.reconnect(); runCurrent()
        assertEquals(2, games.observeCalls)
        assertEquals(-6L, vm.state.value.game?.match?.ratingChange)
        assertEquals("ranked-result", vm.state.value.game?.id)
        assertEquals(82.0, vm.state.value.game!!.evaluation!!.score, 0.0)
        assertNull(vm.state.value.error)
        assertNoGameMutation(games)
    }

    private fun session(users: UserRepository, auth: AuthRepository) =
        SessionViewModel(ObserveSession(auth), InitializeUser(users), ObserveUser(users), SignIn(auth), SignOut(auth))
            .also { store.put("session", it) }

    private fun game(games: GameRepository): GameViewModel {
        val drafts = MemoryDrafts()
        return GameViewModel("alice", PlayMode.RANKED, StartGame(games), SubmitWriting(games), ObserveGame(games),
            GetRecentGames(games), RetryEvaluation(games), LoadDraft(drafts), SaveDraft(drafts), ClearDraft(drafts), FindDraft(drafts))
            .also { store.put("game", it) }
    }

    private fun assertNoGameMutation(games: FailingGames) {
        assertEquals(0, games.startCalls); assertEquals(0, games.submitCalls); assertEquals(0, games.retryCalls)
    }

    private class ControlledAuth : AuthRepository {
        val uid = MutableStateFlow<String?>("alice")
        var signOutCalls = 0
        override fun observeSession(): Flow<String?> = uid
        override suspend fun signInWithGoogle(token: String) { error("Unexpected sign in") }
        override suspend fun signOut() { signOutCalls++; uid.value = null }
    }

    private class FailingUsers : UserRepository {
        var current = User("alice", "Alice", "alice@example.test", 1000, 145, 4, "alice")
        var failInitialize = false
        var failObserve = false
        var initializeCalls = 0
        var observeCalls = 0
        var updateCalls = 0
        override suspend fun initialize(): User {
            initializeCalls++
            if (failInitialize) throw IOException("injected-profile-initialize")
            return current
        }
        override fun observeUser(id: String): Flow<User> = flow {
            assertEquals("alice", id); observeCalls++
            emit(current)
            if (failObserve) throw IOException("injected-profile-read")
        }
        override suspend fun updateName(name: String) { updateCalls++ }
    }

    private class FailingGames : GameRepository {
        var gameStatus = GameStatus.EVALUATED
        var failObserve = false
        var observeCalls = 0
        var activeObservers = 0
        var startCalls = 0
        var submitCalls = 0
        var retryCalls = 0
        var matchFields: Map<String, Any?> = mapOf("opponentName" to "Rival", "opponentScore" to 60.0, "outcome" to "WIN", "ratingChange" to 4L)
        fun mapped() = GameDto("ranked-result", mapOf(
            "authorId" to "alice", "schemaVersion" to 2, "mode" to "RANKED", "writingMode" to "STANDARD",
            "challenge" to mapOf("words" to listOf("ink", "tree", "life", "word").map {
                mapOf("text" to it, "definition" to "Definition", "sentence" to "Use $it.", "phonetic" to "/$it/", "partOfSpeech" to "noun", "rarity" to "Common")
            }),
            "status" to gameStatus.name, "matchStatus" to if (gameStatus == GameStatus.EVALUATED) "MATCHED" else "NONE",
            "seasonIndex" to 0, "timestamp" to 1791763200000L,
            "content" to if (gameStatus == GameStatus.DRAFT) "" else List(50) { "ink" }.joinToString(" "),
            "evaluation" to if (gameStatus == GameStatus.EVALUATED) mapOf("score" to 82.0, "feedback" to "Controlled evaluated text", "meritEarned" to 195L,
                "spelling" to "S", "vocabulary" to "V", "coherence" to "C", "creativity" to "R", "constraints" to emptyList<Any>()) else null,
            "match" to if (gameStatus == GameStatus.EVALUATED) matchFields else null
        )).toDomain()
        override fun observe(id: String): Flow<Game> = flow {
            assertEquals("ranked-result", id); observeCalls++; activeObservers++
            try {
                emit(mapped())
                if (failObserve) throw IOException("injected-game-read")
            } finally { activeObservers-- }
        }
        override suspend fun recent(): List<Game> = emptyList()
        override suspend fun start(id: String, mode: PlayMode, writingMode: WritingMode): Game { startCalls++; error("Unexpected start") }
        override suspend fun submit(id: String, content: String) { submitCalls++; error("Unexpected submit") }
        override suspend fun retryEvaluation(id: String) { retryCalls++; error("Unexpected retry evaluation") }
    }

    private class MemoryDrafts : DraftRepository {
        private val data = mutableMapOf<String, Draft>()
        override fun load(userId: String, mode: PlayMode): Draft? = null
        override fun find(userId: String, gameId: String): Draft? = data[gameId]
        override fun save(userId: String, mode: PlayMode, draft: Draft) { data[draft.id] = draft }
        override fun clear(userId: String, mode: PlayMode) { data.clear() }
    }
}
