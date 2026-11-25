package com.aether.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class PhaseScheduleTest {

    private val today = "2026-11-25"
    private fun phase(date: String) = Phase(date, "P $date", listOf(ImportedBlock("NET", 540, 60, "Academic"), ImportedBlock("GATE", null, 30, "GATE")), 450, null)

    @Test fun readsStartDatesFromHeadings() {
        assertEquals("2026-11-25", PhaseSchedule.suggestStart("Main routine · 25 November onward", emptyList(), today))
        assertEquals(null, PhaseSchedule.suggestStart("Until NET on 14 December", emptyList(), today))
        assertEquals("2026-12-15", PhaseSchedule.suggestStart("After NET, until GATE", listOf("Until NET on 14 December"), today))
        assertEquals("2027-01-01", PhaseSchedule.suggestStart("After exams", listOf("Until 31 Dec"), today))
        assertEquals("2027-02-06", PhaseSchedule.suggestStart("From 6 February", emptyList(), today))
        assertEquals("2026-12-15", PhaseSchedule.suggestStart("15 December 2026 – GATE", emptyList(), today))
        assertEquals(null, PhaseSchedule.suggestStart("After GATE", emptyList(), today))
    }

    @Test fun picksTheLatestStartedPhase() {
        val phases = listOf(phase("2027-02-22"), phase("2026-12-15"), phase("2027-01-10"))
        assertEquals(null, PhaseSchedule.split(phases, "2026-12-14").first)
        assertEquals("2026-12-15", PhaseSchedule.split(phases, "2026-12-15").first?.startDate)
        val (due, later) = PhaseSchedule.split(phases, "2027-01-20")
        assertEquals("2027-01-10", due?.startDate)
        assertEquals(listOf("2027-02-22"), later.map { it.startDate })
    }

    @Test fun survivesStorage() {
        val phases = listOf(phase("2026-12-15"), phase("2027-02-22"))
        assertEquals(phases, PhaseSchedule.decode(PhaseSchedule.encode(phases)))
        assertEquals(emptyList<Phase>(), PhaseSchedule.decode(""))
    }
}
