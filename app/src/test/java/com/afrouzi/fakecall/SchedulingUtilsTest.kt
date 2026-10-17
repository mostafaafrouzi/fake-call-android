package com.afrouzi.fakecall

import com.afrouzi.fakecall.scheduling.SchedulingUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class SchedulingUtilsTest {

    @Test
    fun computeTriggerTime_addsDelayInMilliseconds() {
        val baseTime = 10_000L
        val delaySec = 10L
        val expected = 20_000L

        val actual = SchedulingUtils.computeTriggerTime(baseTime, delaySec)
        assertEquals(expected, actual)
    }

    @Test
    fun computeTriggerTime_withZeroDelay_returnsBaseTime() {
        val baseTime = 123_456L
        val actual = SchedulingUtils.computeTriggerTime(baseTime, 0L)
        assertEquals(baseTime, actual)
    }

    @Test
    fun computeTriggerTime_withNegativeDelay_clampsToZero() {
        val baseTime = 50_000L
        val actual = SchedulingUtils.computeTriggerTime(baseTime, -5L)
        assertEquals(baseTime, actual)
    }

    @Test
    fun computeCustomDelaySeconds_standardInput() {
        val minutes = 2
        val seconds = 30
        val expected = 150L

        val actual = SchedulingUtils.computeCustomDelaySeconds(minutes, seconds)
        assertEquals(expected, actual)
    }

    @Test
    fun computeCustomDelaySeconds_minutesOnly() {
        val minutes = 5
        val seconds = 0
        val expected = 300L

        val actual = SchedulingUtils.computeCustomDelaySeconds(minutes, seconds)
        assertEquals(expected, actual)
    }

    @Test
    fun computeCustomDelaySeconds_secondsOnly() {
        val minutes = 0
        val seconds = 45
        val expected = 45L

        val actual = SchedulingUtils.computeCustomDelaySeconds(minutes, seconds)
        assertEquals(expected, actual)
    }

    @Test
    fun computeCustomDelaySeconds_negativeClamping() {
        val actual = SchedulingUtils.computeCustomDelaySeconds(-1, -10)
        assertEquals(0L, actual)
    }
}
