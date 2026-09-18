package com.example.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatcherTest {

    @Test
    fun `matches despite e yo mismatch`() {
        assertTrue(SearchMatcher.matches("КотоПёс", "котопес"))
        assertTrue(SearchMatcher.matches("котопес", "КотоПёс"))
    }

    @Test
    fun `matches despite punctuation and brackets in title`() {
        assertTrue(SearchMatcher.matches("КотоПёс (у ветеринара) [HD]", "котопес"))
        assertTrue(SearchMatcher.matches("Мультфильм: КотоПёс, серия 5", "котопес"))
    }

    @Test
    fun `matches despite a missing letter`() {
        assertTrue(SearchMatcher.matches("КотоПёс", "котопс"))
    }

    @Test
    fun `matches despite a wrong letter typo`() {
        assertTrue(SearchMatcher.matches("КотоПёс", "катопес"))
    }

    @Test
    fun `matches multi-word query in a different order and word ending`() {
        assertTrue(SearchMatcher.matches("Стимпи при смерти", "смерть Стимпи"))
    }

    @Test
    fun `matches a partially typed word as a prefix`() {
        assertTrue(SearchMatcher.matches("Стимпи", "стим"))
    }

    @Test
    fun `does not match unrelated titles`() {
        assertFalse(SearchMatcher.matches("Совершенно другое видео про собаку", "котопес"))
        assertFalse(SearchMatcher.matches("Кот и Пёс", "слон"))
    }

    @Test
    fun `blank query matches everything`() {
        assertTrue(SearchMatcher.matches("Любое видео", ""))
        assertTrue(SearchMatcher.matches("Любое видео", "   "))
    }

    @Test
    fun `matchesAny checks title and channel`() {
        assertTrue(SearchMatcher.matchesAny("Другое видео", "КотоПёс", query = "котопес"))
        assertFalse(SearchMatcher.matchesAny("Другое видео", "Другой канал", query = "котопес"))
    }
}
