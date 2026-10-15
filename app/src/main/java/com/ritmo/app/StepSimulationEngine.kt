package com.ritmo.app

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.floor

/** Pure simulation. No random state, sensors, networking or background work. */
class StepSimulationEngine {
    companion object {
        const val GOAL = 7500
        val HISTORY_START: LocalDate = LocalDate.of(2026, 9, 1)
    }
    private val minutes = intArrayOf(0, 360, 540, 720, 900, 1080, 1260, 1425, 1440)
    private val progress = doubleArrayOf(0.0, .02, .20, .38, .55, .75, .92, 1.0, 1.0)
    private fun seed(date: LocalDate): Long {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(date.toString().toByteArray(Charsets.UTF_8))
        return digest.take(4).fold(0L) { h, byte -> (h shl 8) or (byte.toLong() and 255) }
    }
    fun dailyTarget(date: LocalDate): Int {
        return dailyTarget(date, seed(date))
    }
    private fun dailyTarget(date: LocalDate, dailySeed: Long): Int {
        val weekend = date.dayOfWeek.value >= 6
        val min = if (weekend) 7000 else 7800
        val max = if (weekend) 8200 else 9800
        return min + (dailySeed % (max - min + 1)).toInt()
    }
    fun steps(date: LocalDate, now: LocalDateTime): Int {
        if (date < HISTORY_START || date > now.toLocalDate()) return 0
        val dailySeed = seed(date)
        val target = dailyTarget(date, dailySeed)
        if (date < now.toLocalDate()) return target
        val minute = now.toLocalTime().toSecondOfDay() / 60.0
        val i = (0 until minutes.lastIndex).first { minute < minutes[it + 1] }
        val t = ((minute - minutes[i]) / (minutes[i + 1] - minutes[i])).coerceIn(0.0, 1.0)
        // A seeded time warp creates tiny daily fluctuations while remaining monotone.
        // derivative is >= 0.92; smoothstep is also monotone, so integer totals cannot fall.
        val amplitude = ((dailySeed + i * 17) % 17 - 8) / 100.0
        val warped = t + amplitude * t * (1 - t)
        val smooth = warped * warped * (3 - 2 * warped)
        return floor(target * (progress[i] + (progress[i + 1] - progress[i]) * smooth)).toInt().coerceIn(0, target)
    }
}
