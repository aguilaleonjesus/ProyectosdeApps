package com.ritmo.app

import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime

/** Persist each day's high-water mark across clock changes and app restarts. */
class StepRepository(context: Context, val engine: StepSimulationEngine = StepSimulationEngine()) {
    private val prefs = context.getSharedPreferences("ritmo_progress", Context.MODE_PRIVATE)
    private val store = object : StepProgressStore {
        override fun read(date: LocalDate): Int = prefs.getInt("day_$date", 0)
        override fun write(date: LocalDate, steps: Int) {
            prefs.edit().putInt("day_$date", steps).apply()
        }
    }

    private val stepHistory = StepHistory(engine, store, readLegacyProgress())

    init {
        if (prefs.contains("date") || prefs.contains("steps")) {
            prefs.edit().remove("date").remove("steps").apply()
        }
    }

    private fun readLegacyProgress(): DaySteps? {
        val date = prefs.getString("date", null) ?: return null
        return runCatching { DaySteps(LocalDate.parse(date), prefs.getInt("steps", 0)) }.getOrNull()
    }

    fun steps(date: LocalDate, now: LocalDateTime): Int = stepHistory.steps(date, now)
    fun days(start: LocalDate, count: Int, now: LocalDateTime): List<DaySteps> =
        stepHistory.days(start, count, now)
    fun history(now: LocalDateTime): List<DaySteps> = stepHistory.history(now)
}
