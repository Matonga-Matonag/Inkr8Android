package com.inkr8.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal class FirebaseAuthDataSource(private val auth: FirebaseAuth) {
    fun observeSession() = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    suspend fun signIn(token: String) { auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await() }
    fun signOut() = auth.signOut()
    fun userId(): String = requireNotNull(auth.currentUser?.uid) { "Inicia sesión para continuar." }
}
