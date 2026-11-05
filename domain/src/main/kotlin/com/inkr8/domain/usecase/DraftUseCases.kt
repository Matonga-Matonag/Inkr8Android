package com.inkr8.domain.usecase

import com.inkr8.domain.model.PlayMode
import com.inkr8.domain.repository.Draft
import com.inkr8.domain.repository.DraftRepository

class LoadDraft(private val repository: DraftRepository) {
    operator fun invoke(userId: String, mode: PlayMode): Draft? = repository.load(userId, mode)
}

class SaveDraft(private val repository: DraftRepository) {
    operator fun invoke(userId: String, mode: PlayMode, draft: Draft) = repository.save(userId, mode, draft)
}

class ClearDraft(private val repository: DraftRepository) {
    operator fun invoke(userId: String, mode: PlayMode) = repository.clear(userId, mode)
}

class FindDraft(private val repository: DraftRepository) {
    operator fun invoke(userId: String, gameId: String): Draft? = repository.find(userId, gameId)
}
