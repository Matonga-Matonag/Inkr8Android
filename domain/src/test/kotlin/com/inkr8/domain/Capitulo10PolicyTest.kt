package com.inkr8.domain

import com.inkr8.domain.model.WritingMode
import com.inkr8.domain.policy.GamePolicy
import org.junit.Assert.*
import org.junit.Test

class Capitulo10PolicyTest {
    private fun words(n: Int) = List(n) { "ink" }.joinToString(" ")
    @Test fun AND_PED_C01_standard49Rejected() { assertNotNull(GamePolicy.validateWriting(words(49), WritingMode.STANDARD)) }
    @Test fun AND_PED_C02_standard50Accepted() { assertNull(GamePolicy.validateWriting(words(50), WritingMode.STANDARD)) }
    @Test fun AND_PED_C03_standard150Accepted() { assertNull(GamePolicy.validateWriting(words(150), WritingMode.STANDARD)) }
    @Test fun AND_PED_C04_standard151Rejected() { assertNotNull(GamePolicy.validateWriting(words(151), WritingMode.STANDARD)) }
    @Test fun AND_PED_C05_topic200Accepted() { assertNull(GamePolicy.validateWriting(words(200), WritingMode.ON_TOPIC)) }
    @Test fun AND_PED_C06_topic201Rejected() { assertNotNull(GamePolicy.validateWriting(words(201), WritingMode.ON_TOPIC)) }
    @Test fun AND_PED_C07_characters10000Accepted() {
        val text = List(50) { "x".repeat(199) }.joinToString(" ") + "x"
        assertEquals(10000, text.length); assertEquals(50, GamePolicy.wordCount(text))
        assertNull(GamePolicy.validateWriting(text, WritingMode.STANDARD))
    }
    @Test fun AND_PED_C08_characters10001RejectedWith50Words() {
        val text = List(50) { "x".repeat(199) }.joinToString(" ") + "xx"
        assertEquals(10001, text.length); assertEquals(50, GamePolicy.wordCount(text))
        assertEquals("El texto excede el límite de caracteres.", GamePolicy.validateWriting(text, WritingMode.STANDARD))
    }
}
