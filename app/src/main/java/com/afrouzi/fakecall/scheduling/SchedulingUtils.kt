package com.afrouzi.fakecall.scheduling

object SchedulingUtils {

    fun computeTriggerTime(baseMillis: Long, delaySeconds: Long): Long {
        val validDelay = if (delaySeconds < 0L) 0L else delaySeconds
        return baseMillis + validDelay * 1000L
    }

    fun computeCustomDelaySeconds(minutes: Int, seconds: Int): Long {
        val validMinutes = if (minutes < 0) 0 else minutes
        val validSeconds = if (seconds < 0) 0 else seconds
        return validMinutes * 60L + validSeconds
    }
}
