package com.inkr8.domain.policy

import com.inkr8.domain.model.Season
import com.inkr8.domain.model.WritingMode

object GamePolicy {
    const val RANKED_COST = 100L
    const val INITIAL_MERIT = 1000L
    fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    fun validateWriting(text: String, mode: WritingMode): String? = when {
        text.length > 10000 -> "El texto excede el límite de caracteres."
        wordCount(text) !in mode.minWords..mode.maxWords -> "Escribe entre ${mode.minWords} y ${mode.maxWords} palabras."
        else -> null
    }
    fun league(rating: Long): Int = when {
        rating < 30 -> 1; rating < 60 -> 2; rating < 90 -> 3
        rating < 120 -> 4; rating < 150 -> 5; else -> 6
    }
}

object SeasonPolicy {
    const val ANCHOR_MS = 1791763200000L // 2026-10-12T00:00:00Z
    const val DURATION_MS = 14L * 24 * 60 * 60 * 1000
    fun at(now: Long): Season {
        val index = Math.floorDiv(now - ANCHOR_MS, DURATION_MS)
        val start = ANCHOR_MS + index * DURATION_MS
        return Season("s$index", start, start + DURATION_MS)
    }
}
