package com.aether.core.engine

import com.aether.core.model.ALL_DAYS
import com.aether.core.model.Routine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimetableImporterTest {

    private val sample = """
### Your daily timetable

Main routine · 25 November onward

| Time             | Activity                                        |
| ---------------- | ----------------------------------------------- |
| 7:30 AM          | Wake up, freshen up, water                      |
| 8:00–9:00        | Breakfast                                       |
| 9:00–10:45       | NET Paper 2 – main subject study                |
| 10:45–11:00      | Short break                                     |
| 11:00–1:00       | College work / TA work / assignments / research |
| 1:00–2:00        | Lunch + rest                                    |
| 2:00–3:30        | NET Paper 1 + PYQs                              |
| 3:30–4:00        | Tea / walk / break                              |
| 4:00–5:00        | GATE preparation – core subject revision        |
| 5:00–6:30        | Badminton                                       |
| 6:30–7:00        | Freshen up + relax                              |
| 7:00–7:45        | Internship preparation / research work          |
| 8:00–9:00        | Dinner                                          |
| 9:00–10:30       | PYQs / mock test analysis / revision            |
| 10:30–11:00      | Guitar 🎸                                       |
| 11:00–11:30      | Relax, plan tomorrow, no unnecessary scrolling  |
| 11:30 PM–7:30 AM | Sleep                                           |

### How to divide your preparation

| Area                  | Daily target |
| --------------------- | ------------ |
| NET Paper 2           | 1 hr 45 min  |
| GATE                  | 1 hour       |
| College work          | As required  |

| Day       | Evening activity       | Extra focus          |
| --------- | ---------------------- | -------------------- |
| Monday    | Badminton              | Discrete Mathematics |
| Tuesday   | Badminton              | DSA + Algorithms     |
| Sunday    | Rest / casual game     | Mock test + analysis |

### Three rules I want you to follow

* No zero days: at least one 45-minute session.

* Don't sacrifice sleep.

### Until NET on 14 December

| Time             | Work                     | Duration |
| ---------------- | ------------------------ | -------- |
| 7:00–9:30 AM     | NET Paper 2              | 2.5 h    |
| 11:30 AM–1:00 PM | NET PYQs / mock analysis | 1.5 h    |
| 3:00–4:30 PM     | GATE maintenance         | 1.5 h    |
| 9:00–9:30 PM     | Internship skills        | 0.5 h    |

| Days  | Paper 2           | Paper 1  |
| ----- | ----------------- | -------- |
| 1–3   | Discrete Maths    | Teaching |
| 4–6   | Algorithms + DBMS | Logical  |
"""

    private fun at(h: Int, m: Int = 0) = h * 60 + m

    @Test fun readsTimesWithoutAmPmInOrder() {
        val main = TimetableImporter.parse(sample).timetables.first()
        assertEquals("Main routine · 25 November onward", main.title)
        assertEquals(
            listOf(at(7, 30), at(8), at(9), at(10, 45), at(11), at(13), at(14), at(15, 30), at(16), at(17), at(18, 30), at(19), at(20), at(21), at(22, 30), at(23)),
            main.blocks.map { it.startMinute }
        )
        assertEquals(120, main.blocks[4].minutes) // 11:00–1:00
        assertEquals(30, main.blocks[0].minutes)  // single time lasts until the next row
        assertEquals(at(7, 30), main.wakeMinute)
        assertEquals(at(23, 30), main.sleepMinute)
    }

    @Test fun sortsActivitiesIntoDomains() {
        val byTitle = TimetableImporter.parse(sample).timetables.first().blocks.associate { it.title.substringBefore(" ") to it.domain }
        assertEquals("Break", byTitle["Breakfast"])
        assertEquals("Break", byTitle["Lunch"])
        assertEquals("Academic", byTitle["NET"])
        assertEquals("College Work", byTitle["College"])
        assertEquals("GATE", byTitle["GATE"])
        assertEquals("Gym", byTitle["Badminton"])
        assertEquals("Career", byTitle["Internship"])
        assertEquals("Personal", byTitle["Guitar"])
    }

    @Test fun findsEveryTableAndIgnoresDayCounts() {
        val result = TimetableImporter.parse(sample)
        assertEquals(3, result.timetables.size)
        assertEquals(listOf(105, 60), result.timetables[1].blocks.map { it.minutes })
        val exam = result.timetables[2]
        assertEquals("Until NET on 14 December", exam.title)
        assertEquals(listOf(at(7), at(11, 30), at(15), at(21)), exam.blocks.map { it.startMinute })
        assertEquals("Badminton · Discrete Mathematics", result.weeks.single().byDay[0])
        assertEquals(setOf(0, 1, 6), result.weeks.single().byDay.keys)
        assertEquals(2, result.rules.size)
    }

    @Test fun readsPlainLinesAndTimeOnItsOwnLine() {
        val plain = TimetableImporter.parse("6 AM Run\n9-11 am Maths\n2 to 4 pm Lab").timetables.single()
        assertEquals(listOf(at(6), at(9), at(14)), plain.blocks.map { it.startMinute })
        val split = TimetableImporter.parse("9:00–10:45\nNET Paper 2\n11:00–1:00\nCollege work").timetables.single()
        assertEquals(listOf("NET Paper 2", "College work"), split.blocks.map { it.title })
        assertTrue(TimetableImporter.parse("I want to study more this week.").isEmpty)
    }

    @Test fun importedBlocksRingButOldCommitmentsStaySilent() {
        val mine = Routine("a", "NET", "Academic", 60, ALL_DAYS, at(9), 0, remind = true)
        val college = Routine("b", "College", "College Work", 60, ALL_DAYS, at(11), 0)
        val slots = TimetablePlanner().plan(listOf(mine, college), 7, 23, null).slots.filter { it.dayOfWeek == 0 }
        assertEquals(listOf("rt_0_a", "fx_0_b"), slots.map { it.id })
    }
}
