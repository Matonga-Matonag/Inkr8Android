package com.inkr8.presentation

import com.inkr8.data.dto.GameDto
import com.inkr8.data.mapper.toDomain
import com.inkr8.data.mapper.toWord
import com.inkr8.domain.model.Word
import org.junit.Assert.*
import org.junit.Test

class Capitulo10MappingTest {
    private val word = mapOf<String, Any?>("text" to "ink", "definition" to "Fluid", "sentence" to "Use ink.", "phonetic" to "/ink/", "partOfSpeech" to "noun", "rarity" to "Common")
    @Test fun AND_REN_C01_toWordReceivesAndPreservesSixFields() {
        assertEquals(Word("ink", "Fluid", "Use ink.", "/ink/", "noun", "Common"), word.toWord())
    }
    @Test fun AND_REN_C02_toWordMissingMetadataUsesDocumentedEmptyDefaults() {
        val result = mapOf<String, Any?>("text" to "ink").toWord()
        assertEquals("ink", result.text); assertEquals("", result.definition); assertEquals("", result.phonetic)
        assertEquals("", result.partOfSpeech); assertEquals("", result.rarity); assertEquals("", result.sentence)
    }
    @Test fun AND_REN_C03_toWordWrongWireTypesUseEmptyDefaults() {
        assertEquals(Word("", "", "", "", "", ""), word.mapValues { 42 }.toWord())
    }
    @Test fun AND_REN_C04_eachReceivedWordFieldInfluencesItsOutput() {
        val keys = listOf("text", "definition", "sentence", "phonetic", "partOfSpeech", "rarity")
        keys.forEachIndexed { index, key ->
            val mapped = (word + (key to "changed")).toWord()
            val values = listOf(mapped.text, mapped.definition, mapped.sentence, mapped.phonetic, mapped.partOfSpeech, mapped.rarity)
            assertEquals(key, "changed", values[index])
        }
    }
    private fun game(match: Map<String, Any?>) = GameDto("g", mapOf(
        "challenge" to mapOf("words" to emptyList<Any>()), "mode" to "RANKED", "writingMode" to "STANDARD",
        "status" to "EVALUATED", "matchStatus" to "MATCHED", "seasonIndex" to 0,
        "evaluation" to mapOf("score" to 74.0, "feedback" to "Base", "spelling" to "S", "vocabulary" to "V", "coherence" to "C", "creativity" to "R", "constraints" to emptyList<Any>()),
        "match" to match
    )).toDomain()
    @Test fun AND_JEA_C01_winRatingAndOpponentAreMapped() {
        val result = game(mapOf("outcome" to "WIN", "opponentName" to "Bob", "opponentScore" to 60.0, "ratingChange" to 4))
        assertEquals("WIN", result.match?.outcome); assertEquals(4L, result.match?.ratingChange); assertEquals("Bob", result.match?.opponentName)
    }
    @Test fun AND_JEA_C02_lossPreservesNegativeVariation() {
        val result = game(mapOf("outcome" to "LOSS", "opponentScore" to 80.0, "ratingChange" to -6))
        assertEquals("LOSS", result.match?.outcome); assertEquals(-6L, result.match?.ratingChange)
    }
    @Test fun AND_JEA_C03_drawDoesNotInventMissingVariation() {
        val result = game(mapOf("outcome" to "DRAW", "opponentScore" to 74.0))
        assertEquals("DRAW", result.match?.outcome); assertNull(result.match?.ratingChange)
    }
    @Test fun AND_JEA_C04_malformedMatchIsNotDisplayedAsValid() {
        assertNull(game(mapOf("outcome" to "OTHER", "opponentScore" to 74)).match)
        assertNull(game(mapOf("outcome" to "WIN")).match)
    }
}
