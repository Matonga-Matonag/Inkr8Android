package com.inkr8.domain.usecase

import com.inkr8.domain.repository.AuthRepository

class ObserveSession(private val repository: AuthRepository) { operator fun invoke() = repository.observeSession() }
class SignIn(private val repository: AuthRepository) { suspend operator fun invoke(token: String) = repository.signInWithGoogle(token) }
class SignOut(private val repository: AuthRepository) { suspend operator fun invoke() = repository.signOut() }
