package com.inkr8.domain

import com.inkr8.domain.model.WritingMode
import com.inkr8.domain.policy.GamePolicy
import com.inkr8.domain.policy.SeasonPolicy
import org.junit.Assert.*
import org.junit.Test

class GamePolicyTest {
    @Test fun fourteenDayBoundaryIncludesNegativePeriods() {
        val start = SeasonPolicy.ANCHOR_MS
        assertEquals("s-1", SeasonPolicy.at(start - 1).id)
        assertEquals("s0", SeasonPolicy.at(start).id)
        assertEquals("s0", SeasonPolicy.at(start + SeasonPolicy.DURATION_MS - 1).id)
        assertEquals("s1", SeasonPolicy.at(start + SeasonPolicy.DURATION_MS).id)
        assertEquals(SeasonPolicy.DURATION_MS, SeasonPolicy.at(start).end - start)
    }
    @Test fun onlyNumericLeaguesOneToSixAreReturned() {
        assertEquals(listOf(1,1,2,3,4,5,6,6), listOf(0L,29,30,60,90,120,150,1000).map(GamePolicy::league))
    }
    @Test fun writingLimitsAndWhitespaceAreValidated() {
        fun text(count: Int) = List(count) { "palabra" }.joinToString(" \n ")
        assertEquals(50, GamePolicy.wordCount(text(50)))
        assertNull(GamePolicy.validateWriting(text(50), WritingMode.STANDARD))
        assertNull(GamePolicy.validateWriting(text(150), WritingMode.STANDARD))
        assertNotNull(GamePolicy.validateWriting(text(151), WritingMode.STANDARD))
        assertNotNull(GamePolicy.validateWriting(" ", WritingMode.STANDARD))
        assertNull(GamePolicy.validateWriting(text(200), WritingMode.ON_TOPIC))
    }
}
