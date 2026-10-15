package com.ritmo.app

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class DaySteps(val date: LocalDate, val steps: Int)

/** Stores the highest displayed total for each date, independently of the clock. */
interface StepProgressStore {
    fun read(date: LocalDate): Int
    fun write(date: LocalDate, steps: Int)
}

/** Date and persistence rules kept independent of Android for deterministic validation. */
class StepHistory(
    val engine: StepSimulationEngine,
    private val store: StepProgressStore,
    legacyProgress: DaySteps? = null,
) {
    init {
        // The previous app remembered only one date. Preserve its best known total.
        legacyProgress?.takeIf { it.date >= StepSimulationEngine.HISTORY_START }?.let { previous ->
            val restored = maxOf(store.read(previous.date), previous.steps)
                .coerceIn(0, engine.dailyTarget(previous.date))
            if (restored > store.read(previous.date)) store.write(previous.date, restored)
        }
    }

    @Synchronized
    fun steps(date: LocalDate, now: LocalDateTime): Int {
        // A stored total must never make a future or unsupported day appear active.
        if (date < StepSimulationEngine.HISTORY_START || date > now.toLocalDate()) return 0
        val previous = store.read(date)
        val result = maxOf(previous, engine.steps(date, now))
            .coerceIn(0, engine.dailyTarget(date))
        if (result != previous) store.write(date, result)
        return result
    }

    fun days(start: LocalDate, count: Int, now: LocalDateTime): List<DaySteps> {
        require(count >= 0) { "Day count must be nonnegative" }
        return List(count) { offset ->
            val date = start.plusDays(offset.toLong())
            DaySteps(date, steps(date, now))
        }
    }

    fun history(now: LocalDateTime): List<DaySteps> {
        val today = now.toLocalDate()
        if (today < StepSimulationEngine.HISTORY_START) return emptyList()
        val count = ChronoUnit.DAYS.between(StepSimulationEngine.HISTORY_START, today) + 1
        return days(StepSimulationEngine.HISTORY_START, Math.toIntExact(count), now)
    }
}
