package com.inkr8.data.repository

import com.inkr8.data.source.FirebaseAuthDataSource
import com.inkr8.data.source.GoogleSignInDataSource
import com.inkr8.domain.repository.AuthRepository

internal class FirebaseAuthRepository(private val source: FirebaseAuthDataSource, private val google: GoogleSignInDataSource) : AuthRepository {
    override fun observeSession() = source.observeSession()
    override suspend fun signInWithGoogle(token: String) = source.signIn(token)
    override suspend fun signOut() { source.signOut(); google.signOut() }
}
