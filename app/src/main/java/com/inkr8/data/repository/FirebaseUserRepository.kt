package com.inkr8.data.repository

import com.inkr8.data.dto.*
import com.inkr8.data.mapper.toDomain
import com.inkr8.data.source.FirestoreDataSource
import com.inkr8.data.source.FunctionsDataSource
import com.inkr8.domain.model.User
import com.inkr8.domain.repository.UserRepository
import kotlinx.coroutines.flow.map

internal class FirebaseUserRepository(private val functions: FunctionsDataSource, private val firestore: FirestoreDataSource) : UserRepository {
    override suspend fun initialize(): User {
        val response = functions.call("initializeUser")
        return UserDto.read(response.string("id"), response).toDomain()
    }
    override fun observeUser(id: String) = firestore.observeDocument("users/$id").map { UserDto.read(id, it).toDomain() }
    override suspend fun updateName(name: String) { functions.call("updateProfile", mapOf("name" to name)) }
}
