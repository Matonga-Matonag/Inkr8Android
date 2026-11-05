package com.inkr8.domain.repository

import com.inkr8.domain.model.PlayMode
import com.inkr8.domain.model.WritingMode

data class Draft(val id: String, val writingMode: WritingMode, val text: String)
interface DraftRepository {
    fun load(userId: String, mode: PlayMode): Draft?
    fun find(userId: String, gameId: String): Draft?
    fun save(userId: String, mode: PlayMode, draft: Draft)
    fun clear(userId: String, mode: PlayMode)
}
