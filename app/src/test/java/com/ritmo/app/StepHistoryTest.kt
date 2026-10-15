package com.ritmo.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class StepHistoryTest {
    private class MemoryStore : StepProgressStore {
        val totals = mutableMapOf<LocalDate, Int>()
        override fun read(date: LocalDate): Int = totals[date] ?: 0
        override fun write(date: LocalDate, steps: Int) { totals[date] = steps }
    }

    private val engine = StepSimulationEngine()
    private val store = MemoryStore()
    private val history = StepHistory(engine, store)
    private val date = LocalDate.of(2026, 10, 14)

    @Test fun historyContainsEveryDateSinceSeptemberFirstIncludingToday() {
        val records = history.history(date.atTime(12, 0))
        assertEquals(44, records.size)
        assertEquals(LocalDate.of(2026, 9, 1), records.first().date)
        assertEquals(date, records.last().date)
        records.zipWithNext().forEach { (first, second) ->
            assertEquals(first.date.plusDays(1), second.date)
        }
        records.dropLast(1).forEach { day ->
            assertEquals(engine.dailyTarget(day.date), day.steps)
            assertTrue("${day.date}", day.steps >= 7000)
            if (day.date.dayOfWeek.value <= 5) assertTrue(day.steps >= 7800)
        }
        assertEquals(engine.steps(date, date.atTime(12, 0)), records.last().steps)
    }

    @Test fun historyBeforeItsStartIsEmpty() {
        assertTrue(history.history(StepSimulationEngine.HISTORY_START.minusDays(1).atTime(23, 59)).isEmpty())
        assertEquals(1, history.history(StepSimulationEngine.HISTORY_START.atStartOfDay()).size)
    }

    @Test fun timeRollbackWithinDayKeepsHighestDisplayedTotal() {
        val later = history.steps(date, date.atTime(18, 0))
        assertEquals(later, history.steps(date, date.atTime(9, 0)))
        assertEquals(later, history.steps(date, date.atStartOfDay()))
    }

    @Test fun dateRollbackAndRestartKeepEachDatesOwnHighWaterMark() {
        val first = history.steps(date, date.atTime(18, 0))
        val next = date.plusDays(1)
        val second = history.steps(next, next.atTime(21, 0))
        val restarted = StepHistory(StepSimulationEngine(), store)
        assertEquals(first, restarted.steps(date, date.atTime(8, 0)))
        assertEquals(second, restarted.steps(next, next.atTime(8, 0)))
        assertNotEquals(store.read(date), store.read(next))
    }

    @Test fun viewedCompletedDayRemainsCompleteAfterClockRollsBack() {
        val complete = history.steps(date, date.plusDays(1).atStartOfDay())
        assertEquals(engine.dailyTarget(date), complete)
        assertEquals(complete, history.steps(date, date.atTime(12, 0)))
    }

    @Test fun futureDayIsZeroEvenWhenItsProgressWasPreviouslyStored() {
        val future = date.plusDays(1)
        val recorded = history.steps(future, future.atTime(18, 0))
        assertTrue(recorded > 0)
        assertEquals(0, history.steps(future, date.atTime(18, 0)))
        assertEquals(recorded, history.steps(future, future.atTime(9, 0)))
    }

    @Test fun migratesLegacyTodaysProgressWithoutLoweringExistingValue() {
        val legacySteps = engine.steps(date, date.atTime(18, 0))
        val migrated = StepHistory(engine, store, DaySteps(date, legacySteps))
        assertEquals(legacySteps, migrated.steps(date, date.atTime(6, 0)))
        val restarted = StepHistory(engine, store, DaySteps(date, 1))
        assertEquals(legacySteps, restarted.steps(date, date.atTime(6, 0)))
    }

    @Test fun restoredTotalsAreClampedAndUnsupportedLegacyDateIsIgnored() {
        val migrated = StepHistory(engine, store, DaySteps(date, Int.MAX_VALUE))
        assertEquals(engine.dailyTarget(date), migrated.steps(date, date.atTime(6, 0)))
        val unsupported = StepSimulationEngine.HISTORY_START.minusDays(1)
        val withOldDate = StepHistory(engine, store, DaySteps(unsupported, 9000))
        assertEquals(0, withOldDate.steps(unsupported, date.atTime(12, 0)))
        assertFalse(store.totals.containsKey(unsupported))
    }

    @Test fun calendarRangesIncludeZeroForUnsupportedAndFutureDays() {
        val start = StepSimulationEngine.HISTORY_START
        val records = history.days(start.minusDays(1), 4, start.plusDays(1).atTime(12, 0))
        assertEquals(4, records.size)
        assertEquals(0, records[0].steps)
        assertEquals(engine.dailyTarget(start), records[1].steps)
        assertTrue(records[2].steps in 1 until engine.dailyTarget(start.plusDays(1)))
        assertEquals(0, records[3].steps)
        assertTrue(history.days(start, 0, LocalDateTime.of(2026, 10, 14, 12, 0)).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeCalendarRangeIsRejected() {
        history.days(date, -1, date.atStartOfDay())
    }
}
