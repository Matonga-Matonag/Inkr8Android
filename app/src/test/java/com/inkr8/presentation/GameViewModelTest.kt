package com.inkr8.presentation

import androidx.lifecycle.ViewModelStore
import com.inkr8.domain.model.*
import com.inkr8.domain.repository.*
import com.inkr8.domain.usecase.*
import com.inkr8.presentation.viewmodel.GameViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val games = FakeGames()
    private val drafts = FakeDrafts()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun vm(user: String = "alice", key: String = "game"): GameViewModel =
        GameViewModel(user, PlayMode.RANKED, StartGame(games), SubmitWriting(games), ObserveGame(games), GetRecentGames(games), RetryEvaluation(games),
            LoadDraft(drafts), SaveDraft(drafts), ClearDraft(drafts), FindDraft(drafts)).also { store.put(key, it) }

    @Test fun networkRetryReusesTheSameRankedRequestId() = runTest(dispatcher) {
        games.failStart = true
        val vm = vm(); runCurrent()
        vm.start(); runCurrent()
        val originalId = vm.state.value.requestId
        assertNotNull(vm.state.value.error)
        games.failStart = false
        vm.start(); runCurrent()
        assertEquals(listOf(originalId, originalId), games.startIds)
        assertEquals(originalId, vm.state.value.game?.id)
    }
    @Test fun doubleSubmitIsBlockedAndDraftSurvivesViewModelRecreation() = runTest(dispatcher) {
        val first = vm(); runCurrent(); first.start(); runCurrent()
        val text = List(50) { "palabra" }.joinToString(" ")
        first.edit(text)
        val second = vm(key = "second"); runCurrent()
        assertEquals(text, second.state.value.text)
        assertEquals(50, second.state.value.wordCount)
        assertTrue(second.state.value.canSubmit)
        second.send(); second.send(); runCurrent()
        assertEquals(1, games.submitCalls)
    }
    @Test fun selectingAnotherGameCancelsOldObserverAndLogoutClearsListeners() = runTest(dispatcher) {
        val vm = vm(); runCurrent(); vm.start(); runCurrent()
        assertEquals(1, games.observers)
        vm.newGame(); runCurrent()
        assertEquals(0, games.observers)
        vm.start(); runCurrent()
        assertEquals(1, games.observers)
        store.clear(); runCurrent()
        assertEquals(0, games.observers)
    }
    @Test fun draftsAreIsolatedByUserAndCanBeReopenedWithoutLosingText() = runTest(dispatcher) {
        val vm = vm(); runCurrent(); vm.start(); runCurrent()
        val game = vm.state.value.game!!
        vm.edit("Borrador local")
        vm.newGame(); runCurrent(); vm.open(game); runCurrent()
        assertEquals("Borrador local", vm.state.value.text)
        assertEquals(2, vm.state.value.wordCount)
        assertFalse(vm.state.value.canSubmit)
        val other = vm(user = "bob", key = "bob"); runCurrent()
        assertNull(other.state.value.game)
        assertEquals("", other.state.value.text)
    }
    @Test fun validationTracksTextLimitsAndBlocksInvalidSubmission() = runTest(dispatcher) {
        val vm = vm(); runCurrent(); vm.start(); runCurrent()
        assertEquals(0, vm.state.value.wordCount)
        assertFalse(vm.state.value.canSubmit)
        vm.edit(List(49) { "palabra" }.joinToString(" ")); vm.send(); runCurrent()
        assertEquals(49, vm.state.value.wordCount)
        assertFalse(vm.state.value.canSubmit)
        assertEquals(0, games.submitCalls)
        vm.edit(List(50) { "palabra" }.joinToString(" \n "))
        assertEquals(50, vm.state.value.wordCount)
        assertTrue(vm.state.value.canSubmit)
        vm.edit(List(151) { "palabra" }.joinToString(" "))
        assertEquals(151, vm.state.value.wordCount)
        assertFalse(vm.state.value.canSubmit)
        vm.newGame(); runCurrent()
        assertEquals(0, vm.state.value.wordCount)
        assertFalse(vm.state.value.canSubmit)
    }
    @Test fun restoredDraftAndServerChangesRecalculateValidation() = runTest(dispatcher) {
        val text = List(180) { "palabra" }.joinToString(" ")
        val game = Game("restored", PlayMode.RANKED, WritingMode.ON_TOPIC, Challenge(emptyList(), null, null),
            GameStatus.DRAFT, "", null, null, null, "s0")
        games.games[game.id] = game
        drafts.save("alice", PlayMode.RANKED, Draft(game.id, game.writingMode, text))
        val vm = vm(); runCurrent()
        assertEquals(180, vm.state.value.wordCount)
        assertTrue(vm.state.value.canSubmit)
        games.update(game.copy(writingMode = WritingMode.STANDARD)); runCurrent()
        assertEquals(text, vm.state.value.text)
        assertFalse(vm.state.value.canSubmit)
        games.update(game.copy(status = GameStatus.PENDING, content = "Texto enviado")); runCurrent()
        assertEquals(2, vm.state.value.wordCount)
        assertFalse(vm.state.value.canSubmit)
        vm.send(); runCurrent()
        assertEquals(0, games.submitCalls)
    }
    private class FakeGames : GameRepository {
        val startIds = mutableListOf<String>()
        val games = mutableMapOf<String, Game>()
        var failStart = false
        var submitCalls = 0
        var observers = 0
        private val updates = MutableSharedFlow<Game>(extraBufferCapacity = 1)
        fun update(game: Game) { games[game.id] = game; check(updates.tryEmit(game)) }
        override suspend fun start(id: String, mode: PlayMode, writingMode: WritingMode): Game {
            startIds += id
            if (failStart) throw IOException("Sin conexión")
            return games.getOrPut(id) { Game(id, mode, writingMode, Challenge(emptyList(), null, null), GameStatus.DRAFT, "", null, null, null, "s0") }
        }
        override suspend fun submit(id: String, content: String) { submitCalls++ }
        override suspend fun retryEvaluation(id: String) = Unit
        override fun observe(id: String): Flow<Game> = flow {
            observers++
            try {
                emit(games.getValue(id))
                updates.filter { it.id == id }.collect { emit(it) }
            } finally { observers-- }
        }
        override suspend fun recent() = games.values.toList()
    }
    private class FakeDrafts : DraftRepository {
        private val active = mutableMapOf<String, Draft>()
        private val all = mutableMapOf<String, Draft>()
        override fun load(userId: String, mode: PlayMode) = active["$userId$mode"]
        override fun find(userId: String, gameId: String) = all["$userId$gameId"]
        override fun save(userId: String, mode: PlayMode, draft: Draft) { active["$userId$mode"] = draft; all["$userId${draft.id}"] = draft }
        override fun clear(userId: String, mode: PlayMode) { active.remove("$userId$mode") }
    }
}
