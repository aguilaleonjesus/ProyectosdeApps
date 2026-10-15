package com.ritmo.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class StepSimulationEngineTest {
    private val engine = StepSimulationEngine()
    private val start = StepSimulationEngine.HISTORY_START

    @Test fun weekdayTargetsExceedSevenThousandAndReachGoal() {
        repeat(1461) { offset ->
            val date = start.plusDays(offset.toLong())
            if (date.dayOfWeek.value <= 5) {
                assertTrue("$date", engine.dailyTarget(date) in 7800..9800)
                assertTrue(engine.dailyTarget(date) >= StepSimulationEngine.GOAL)
            }
        }
    }

    @Test fun weekendTargetsHaveSevenThousandMinimum() {
        repeat(1461) { offset ->
            val date = start.plusDays(offset.toLong())
            if (date.dayOfWeek.value >= 6) {
                assertTrue("$date", engine.dailyTarget(date) in 7000..8200)
            }
        }
    }

    @Test fun sameDateAlwaysHasSameTargetAcrossEngineInstances() {
        repeat(366) { offset ->
            val date = start.plusDays(offset.toLong())
            assertEquals(engine.dailyTarget(date), StepSimulationEngine().dailyTarget(date))
        }
    }

    @Test fun everyMinuteOfAYearNeverDecreases() {
        repeat(366) { offset ->
            val date = start.plusDays(offset.toLong())
            var previous = 0
            repeat(1440) { minute ->
                val current = engine.steps(date, date.atStartOfDay().plusMinutes(minute.toLong()))
                assertTrue("$date minute $minute: $previous -> $current", current >= previous)
                previous = current
            }
        }
    }

    @Test fun countsRemainWithinDailyTargetIncludingSegmentBoundaries() {
        val minutes = listOf(0, 1, 359, 360, 361, 539, 540, 541, 719, 720,
            721, 899, 900, 901, 1079, 1080, 1081, 1259, 1260, 1261, 1424, 1425, 1439)
        repeat(1461) { offset ->
            val date = start.plusDays(offset.toLong())
            minutes.forEach { minute ->
                val current = engine.steps(date, date.atStartOfDay().plusMinutes(minute.toLong()))
                assertTrue("$date minute $minute", current in 0..engine.dailyTarget(date))
            }
        }
    }

    @Test fun pastSupportedDatesShowFullTarget() {
        val now = LocalDateTime.of(2026, 10, 14, 12, 0)
        repeat(43) { offset ->
            val date = start.plusDays(offset.toLong())
            assertEquals("$date", engine.dailyTarget(date), engine.steps(date, now))
        }
    }

    @Test fun futureDatesShowZero() {
        val now = LocalDateTime.of(2026, 10, 14, 12, 0)
        assertEquals(0, engine.steps(now.toLocalDate().plusDays(1), now))
        assertEquals(0, engine.steps(LocalDate.of(2028, 2, 29), now))
    }

    @Test fun datesBeforeSeptemberFirstShowZero() {
        val now = LocalDateTime.of(2026, 10, 14, 12, 0)
        assertEquals(0, engine.steps(start.minusDays(1), now))
        assertEquals(0, engine.steps(LocalDate.of(2024, 2, 29), now))
    }

    @Test fun dailyBoundariesStartAtZeroAndFinishAtTarget() {
        assertEquals(0, engine.steps(start, start.atStartOfDay()))
        assertEquals(engine.dailyTarget(start), engine.steps(start, start.atTime(23, 45)))
        assertEquals(engine.dailyTarget(start), engine.steps(start, start.atTime(23, 59, 59)))
    }

    @Test fun currentDayUsesDeterministicPiecewiseProgressRatherThanFinalTarget() {
        val date = LocalDate.of(2026, 10, 14)
        val now = date.atTime(12, 0)
        val current = engine.steps(date, now)
        assertEquals(current, StepSimulationEngine().steps(date, now))
        assertEquals((engine.dailyTarget(date) * .38).toInt(), current)
        assertTrue(current in 1 until StepSimulationEngine.GOAL)
        assertTrue(current < engine.dailyTarget(date) / 2)
    }

    @Test fun yearAndLeapDayBoundariesAreIndependentCalendarDays() {
        val newYear = LocalDateTime.of(2027, 1, 1, 0, 0)
        val yesterday = LocalDate.of(2026, 12, 31)
        assertEquals(engine.dailyTarget(yesterday), engine.steps(yesterday, newYear))
        assertEquals(0, engine.steps(newYear.toLocalDate(), newYear))
        val leapDay = LocalDate.of(2028, 2, 29)
        assertEquals(engine.dailyTarget(leapDay), engine.steps(leapDay, leapDay.plusDays(1).atStartOfDay()))
    }
}
