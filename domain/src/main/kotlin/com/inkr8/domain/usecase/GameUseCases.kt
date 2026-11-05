package com.inkr8.domain.usecase

import com.inkr8.domain.model.*
import com.inkr8.domain.policy.GamePolicy
import com.inkr8.domain.repository.GameRepository

class StartGame(private val repository: GameRepository) {
    suspend operator fun invoke(id: String, mode: PlayMode, writingMode: WritingMode) = repository.start(id, mode, writingMode)
}
class SubmitWriting(private val repository: GameRepository) {
    suspend operator fun invoke(game: Game, text: String) {
        require(game.status == GameStatus.DRAFT) { "Esta partida ya fue enviada." }
        GamePolicy.validateWriting(text, game.writingMode)?.let { throw IllegalArgumentException(it) }
        repository.submit(game.id, text.trim())
    }
}
class ObserveGame(private val repository: GameRepository) { operator fun invoke(id: String) = repository.observe(id) }
class GetRecentGames(private val repository: GameRepository) { suspend operator fun invoke() = repository.recent() }
class RetryEvaluation(private val repository: GameRepository) { suspend operator fun invoke(id: String) = repository.retryEvaluation(id) }
