package com.inkr8.domain.repository

import com.inkr8.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeSession(): Flow<String?>
    suspend fun signInWithGoogle(token: String)
    suspend fun signOut()
}
interface UserRepository {
    suspend fun initialize(): User
    fun observeUser(id: String): Flow<User>
    suspend fun updateName(name: String)
}
interface HomeRepository {
    suspend fun getHome(): Home
    suspend fun getExample(day: Long): String
}
interface SeasonRepository {
    suspend fun getRanking(): SeasonRanking
    suspend fun getHistory(): List<SeasonHistory>
}
interface ProgressRepository { suspend fun getProgress(): Progress }
interface GameRepository {
    suspend fun start(id: String, mode: PlayMode, writingMode: WritingMode): Game
    suspend fun submit(id: String, content: String)
    suspend fun retryEvaluation(id: String)
    fun observe(id: String): Flow<Game>
    suspend fun recent(): List<Game>
}
