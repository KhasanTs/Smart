package com.example.utils

/**
 * Fuzzy, typo- and punctuation-tolerant text matcher used by every search/filter
 * screen in the app (home search, bookmarks search, offline fallback search,
 * search-history suggestions).
 *
 * Plain `String.contains(query, ignoreCase = true)` fails on real-world queries:
 *  - "ё" vs "е" mismatches ("котопес" should find "КотоПёс")
 *  - punctuation/brackets/dashes inside titles ("[HD] Видео (2019) — озвучка")
 *  - typos or missing letters ("копотес" / "котопс" should still find "КотоПёс")
 *  - multi-word queries typed in a different order or grammatical case than the
 *    title ("смерть Стимпи" should find "Стимпи при смерти")
 *
 * [matches] / [matchesAny] address all of the above while staying dependency-free
 * and fast enough to run on every keystroke over a small in-memory video list.
 */
object SearchMatcher {

    private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")

    /** Lowercases, folds ё/Ё to е, and turns any punctuation/symbols into spaces. */
    fun normalize(text: String): String {
        val lower = text.lowercase().replace('ё', 'е')
        return NON_ALPHANUMERIC.replace(lower, " ").trim()
    }

    /** Splits normalized text into words, dropping empty tokens. */
    fun tokenize(text: String): List<String> =
        normalize(text).split(" ").filter { it.isNotBlank() }

    /**
     * True if [target] is a reasonable match for [query]: tolerant of case, ё/е,
     * punctuation/brackets, typos/missing letters, and word order / word endings.
     */
    fun matches(target: String, query: String): Boolean {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return true
        if (target.isBlank()) return false

        // Fast path: compare with spaces removed too, so punctuation differences,
        // ё/е, case and titles that split a name up differently than the query
        // ("Кото Пёс" vs "котопес") all still line up as a plain substring match.
        val targetJoined = normalize(target).replace(" ", "")
        val queryJoined = normalize(trimmedQuery).replace(" ", "")
        if (queryJoined.isBlank()) return true
        if (targetJoined.contains(queryJoined)) return true

        // Fall back to per-word fuzzy matching: every query word must fuzzily
        // match some word in the target, in any order. This is what lets
        // "смерть Стимпи" find "Стимпи при смерти", and lets small typos or
        // different Russian word endings ("смерть" / "смерти") still match.
        val queryTokens = tokenize(trimmedQuery)
        if (queryTokens.isEmpty()) return true
        val targetTokens = tokenize(target)
        if (targetTokens.isEmpty()) return false

        return queryTokens.all { queryToken ->
            targetTokens.any { targetToken -> tokensMatch(queryToken, targetToken) }
        }
    }

    /** True if [query] matches any of [fields] (e.g. a video's title and channel name). */
    fun matchesAny(fields: List<String>, query: String): Boolean {
        if (query.isBlank()) return true
        return fields.any { matches(it, query) }
    }

    /** Convenience overload for the common title+channel case. */
    fun matchesAny(vararg fields: String, query: String): Boolean =
        matchesAny(fields.toList(), query)

    private fun tokensMatch(query: String, target: String): Boolean {
        if (query == target) return true
        // Too short for fuzzy matching to be meaningful without lots of false positives.
        if (query.length <= 2 || target.length <= 2) return false
        // Handles Russian word-ending/case differences ("смерть" ~ "смерти") and
        // partially typed words.
        if (target.startsWith(query) || query.startsWith(target)) return true

        val maxDist = maxAllowedDistance(minOf(query.length, target.length))
        if (kotlin.math.abs(query.length - target.length) > maxDist) return false
        return levenshtein(query, target) <= maxDist
    }

    /** How many single-character edits (typos/missing letters) we tolerate, by word length. */
    private fun maxAllowedDistance(len: Int): Int = when {
        len <= 3 -> 0
        len <= 5 -> 1
        len <= 8 -> 2
        else -> 3
    }

    private fun levenshtein(a: String, b: String): Int {
        val n = a.length
        val m = b.length
        if (n == 0) return m
        if (m == 0) return n

        var prev = IntArray(m + 1) { it }
        var curr = IntArray(m + 1)

        for (i in 1..n) {
            curr[0] = i
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,    // insertion
                    prev[j] + 1,        // deletion
                    prev[j - 1] + cost  // substitution
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[m]
    }
}
