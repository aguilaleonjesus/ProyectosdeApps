package com.ritmo.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class StepSimulationEngineTest {
    private val engine = StepSimulationEngine()
    private val start = LocalDate.of(2024, 1, 1)
    @Test fun weekdayTargets() { repeat(1461) { val d = start.plusDays(it.toLong()); if (d.dayOfWeek.value <= 5) assertTrue(engine.dailyTarget(d) in 7800..9800) } }
    @Test fun weekendTargets() { repeat(1461) { val d = start.plusDays(it.toLong()); if (d.dayOfWeek.value >= 6) assertTrue(engine.dailyTarget(d) in 3800..7200) } }
    @Test fun deterministicTargets() { repeat(366) { val d = start.plusDays(it.toLong()); assertEquals(engine.dailyTarget(d), StepSimulationEngine().dailyTarget(d)) } }
    @Test fun neverDecreases() { repeat(366) { offset -> val d = start.plusDays(offset.toLong()); var previous = 0; repeat(1440) { minute -> val n = engine.steps(d, d.atStartOfDay().plusMinutes(minute.toLong())); assertTrue("$d minute $minute", n >= previous); previous = n } } }
    @Test fun neverExceedsTarget() { repeat(366) { offset -> val d = start.plusDays(offset.toLong()); repeat(1440) { minute -> assertTrue(engine.steps(d, d.atStartOfDay().plusMinutes(minute.toLong())) in 0..engine.dailyTarget(d)) } } }
    @Test fun pastDatesAreComplete() { val now = LocalDateTime.of(2026, 10, 9, 12, 0); val d = now.toLocalDate().minusDays(1); assertEquals(engine.dailyTarget(d), engine.steps(d, now)) }
    @Test fun futureDatesAreZero() { val now = LocalDateTime.of(2026, 10, 9, 12, 0); assertEquals(0, engine.steps(now.toLocalDate().plusDays(1), now)) }
    @Test fun boundaries() { val d = start; assertEquals(0, engine.steps(d, d.atStartOfDay())); assertEquals(engine.dailyTarget(d), engine.steps(d, d.atTime(23, 45))); assertEquals(engine.dailyTarget(d), engine.steps(d, d.atTime(23, 59, 59))) }
    @Test fun progressIsNonlinearAndDeterministic() { val d = start; val now = d.atTime(12, 0); assertEquals(engine.steps(d, now), StepSimulationEngine().steps(d, now)); assertEquals((engine.dailyTarget(d) * .38).toInt(), engine.steps(d, now)) }
}
