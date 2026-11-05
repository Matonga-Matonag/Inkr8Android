package com.inkr8.data.repository

import com.inkr8.data.dto.GameDto
import com.inkr8.data.mapper.toDomain
import com.inkr8.data.source.*
import com.inkr8.domain.model.*
import com.inkr8.domain.repository.GameRepository
import kotlinx.coroutines.flow.map

internal class FirebaseGameRepository(private val functions: FunctionsDataSource, private val firestore: FirestoreDataSource, private val auth: FirebaseAuthDataSource) : GameRepository {
    override suspend fun start(id: String, mode: PlayMode, writingMode: WritingMode) =
        GameDto(id, functions.call("startGame", mapOf("id" to id, "mode" to mode.name, "writingMode" to writingMode.name))).toDomain()
    override suspend fun submit(id: String, content: String) { functions.call("submitGame", mapOf("id" to id, "content" to content)) }
    override suspend fun retryEvaluation(id: String) { functions.call("retryEvaluation", mapOf("id" to id)) }
    override fun observe(id: String) = firestore.observeDocument("submissions/$id").map { GameDto(id, it).toDomain() }
    override suspend fun recent() = firestore.recentGames(auth.userId()).map { it.toDomain() }
}
