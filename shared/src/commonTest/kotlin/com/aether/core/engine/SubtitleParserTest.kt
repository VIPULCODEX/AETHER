package com.aether.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubtitleParserTest {
    private val srt = "1\n00:00:01,000 --> 00:00:03,500\nA cache is a small, fast memory.\n\n2\n00:00:03,600 --> 00:00:06,000\n<i>It sits between the CPU</i>\nand main memory\n\n3\n00:00:20,000 --> 00:00:22,000\n{\\an8}Now direct mapping.\n"
    private val vtt = "WEBVTT\n\n00:01.000 --> 00:03.000\nHello there\n\n01:02:03.250 --> 01:02:05.000 align:start\nLate line\n"

    @Test fun parsesSrtWithTagsAndMultilineCues() {
        val cues = SubtitleParser.parse(srt)
        assertEquals(3, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals("It sits between the CPU and main memory", cues[1].text)
        assertEquals("Now direct mapping.", cues[2].text)
    }

    @Test fun parsesVttShortAndHourTimestamps() {
        val cues = SubtitleParser.parse(vtt)
        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals(((1 * 60 + 2) * 60 + 3) * 1000L + 250, cues[1].startMs)
    }

    @Test fun studyTextBreaksParagraphsOnPausesAndAddsPunctuation() {
        val text = SubtitleParser.toStudyText(SubtitleParser.parse(srt))
        val paragraphs = text.split("\n\n")
        assertEquals(2, paragraphs.size)       // 14 s pause before cue 3
        assertTrue(paragraphs[0].endsWith("memory."))
        assertTrue(paragraphs.all { it.last() in ".!?" })
    }

    @Test fun encodeDecodeRoundTripKeepsTimesAndText() {
        val cues = SubtitleParser.parse(srt)
        val back = SubtitleParser.decode(SubtitleParser.encode(cues))
        assertEquals(cues.map { it.startMs to it.text }, back.map { it.startMs to it.text })
    }

    @Test fun garbageAndEmptyInputAreHarmless() {
        assertTrue(SubtitleParser.parse("").isEmpty())
        assertTrue(SubtitleParser.parse("not a subtitle file\nat all").isEmpty())
        assertTrue(SubtitleParser.decode(null).isEmpty())
    }

    @Test fun anSrtBecomesAStudySourceTheEnginesCanUse() {
        val text = SubtitleParser.toStudyText(SubtitleParser.parse(srt))
        val chunks = StudyEngine().chunk(listOf(com.aether.core.model.StudySource("v", "n", "Lecture", text, 0)))
        assertTrue(StudyEngine().answer("What is a cache?", chunks).found)
    }
}
