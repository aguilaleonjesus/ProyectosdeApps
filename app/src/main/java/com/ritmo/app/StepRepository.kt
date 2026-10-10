package com.ritmo.app

import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime

data class DaySteps(val date: LocalDate, val steps: Int)

/** Persist only today's high-water mark, protecting progress after clock changes/restarts. */
class StepRepository(context: Context, val engine: StepSimulationEngine = StepSimulationEngine()) {
    private val prefs = context.getSharedPreferences("ritmo_progress", Context.MODE_PRIVATE)
    @Synchronized fun steps(date: LocalDate, now: LocalDateTime): Int {
        val calculated = engine.steps(date, now)
        if (date != now.toLocalDate()) return calculated
        val previous = if (prefs.getString("date", "") == date.toString()) prefs.getInt("steps", 0) else 0
        val result = maxOf(previous, calculated).coerceAtMost(engine.dailyTarget(date))
        prefs.edit().putString("date", date.toString()).putInt("steps", result).apply()
        return result
    }
    fun days(start: LocalDate, count: Int, now: LocalDateTime): List<DaySteps> =
        List(count) { val date = start.plusDays(it.toLong()); DaySteps(date, steps(date, now)) }
}
