package com.afrouzi.fakecall

import com.afrouzi.fakecall.data.db.Converters
import com.afrouzi.fakecall.data.model.CallOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun fromCallOutcome_serializesAllOutcomesCorrectly() {
        assertEquals("ANSWERED", converters.fromCallOutcome(CallOutcome.ANSWERED))
        assertEquals("DECLINED", converters.fromCallOutcome(CallOutcome.DECLINED))
        assertEquals("MISSED", converters.fromCallOutcome(CallOutcome.MISSED))
        assertEquals("CANCELLED", converters.fromCallOutcome(CallOutcome.CANCELLED))
    }

    @Test
    fun toCallOutcome_deserializesValidStrings() {
        assertEquals(CallOutcome.ANSWERED, converters.toCallOutcome("ANSWERED"))
        assertEquals(CallOutcome.DECLINED, converters.toCallOutcome("DECLINED"))
        assertEquals(CallOutcome.MISSED, converters.toCallOutcome("MISSED"))
        assertEquals(CallOutcome.CANCELLED, converters.toCallOutcome("CANCELLED"))
    }

    @Test
    fun toCallOutcome_handlesInvalidOrNullStringsGracefully() {
        assertEquals(CallOutcome.MISSED, converters.toCallOutcome(null))
        assertEquals(CallOutcome.MISSED, converters.toCallOutcome("UNKNOWN_OUTCOME"))
        assertEquals(CallOutcome.MISSED, converters.toCallOutcome(""))
    }
}
