package com.aether.core.engine

import com.aether.core.model.ScheduleSlot

/**
 * v1: a naive, deterministic round-robin allocator across selected focus
 * areas. It exists so a timetable is available immediately without any
 * network dependency. Same input (focus areas + free-text context) and
 * output shape (List<ScheduleSlot>) that a future LLM-backed generator
 * will use, so swapping it in later is a drop-in replacement, not a
 * rearchitecture.
 */
class BasicScheduleGenerator {

    private val dailyTimeSlots = listOf(
        "7:00 AM - 8:30 AM",
        "5:00 PM - 6:30 PM",
        "8:30 PM - 9:30 PM"
    )

    fun generate(focusAreas: List<String>): List<ScheduleSlot> {
        if (focusAreas.isEmpty()) return emptyList()

        val slots = mutableListOf<ScheduleSlot>()
        var areaIndex = 0
        var counter = 0

        for (day in 0..6) {
            for (timeLabel in dailyTimeSlots) {
                val domain = focusAreas[areaIndex % focusAreas.size]
                slots.add(
                    ScheduleSlot(
                        id = "slot_${counter++}",
                        dayOfWeek = day,
                        timeLabel = timeLabel,
                        activityLabel = domain,
                        domain = domain
                    )
                )
                areaIndex++
            }
        }
        return slots
    }
}
