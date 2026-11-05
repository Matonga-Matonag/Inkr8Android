package com.inkr8.domain.usecase

import com.inkr8.domain.repository.UserRepository

class InitializeUser(private val repository: UserRepository) { suspend operator fun invoke() = repository.initialize() }
class ObserveUser(private val repository: UserRepository) { operator fun invoke(id: String) = repository.observeUser(id) }
class UpdateName(private val repository: UserRepository) {
    suspend operator fun invoke(name: String) {
        require(name.trim().length in 2..20) { "El nombre debe tener entre 2 y 20 caracteres." }
        repository.updateName(name.trim())
    }
}
