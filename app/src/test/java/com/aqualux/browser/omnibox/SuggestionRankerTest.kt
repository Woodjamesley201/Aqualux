package com.aqualux.browser.omnibox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionRankerTest {
    @Test
    fun blankQueryReturnsNothing() {
        val hits = SuggestionRanker.rank(
            "  ",
            listOf(Suggestion("Example", "https://example.com", SuggestionSource.HISTORY)),
        )
        assertTrue(hits.isEmpty())
    }

    @Test
    fun openTabOutranksHistoryForTheSameUrl() {
        val hits = SuggestionRanker.rank(
            "example",
            listOf(
                Suggestion("Example", "https://example.com", SuggestionSource.HISTORY, recencyEpochMs = 50),
                Suggestion("Example tab", "https://example.com", SuggestionSource.TAB, tabId = "t", recencyEpochMs = 1),
            ),
        )
        assertEquals(1, hits.size)
        assertEquals(SuggestionSource.TAB, hits.first().source)
        assertEquals("t", hits.first().tabId)
    }

    @Test
    fun prefixBeatsAWeakerContainsAndBookmarksBoost() {
        val hits = SuggestionRanker.rank(
            "tide",
            listOf(
                Suggestion("A tide pool guide", "https://pools.example/guide", SuggestionSource.HISTORY, recencyEpochMs = 10),
                Suggestion("Tide", "https://tide.example", SuggestionSource.BOOKMARK, recencyEpochMs = 1),
            ),
        )
        assertEquals("https://tide.example", hits.first().url)
    }

    @Test
    fun limitIsRespected() {
        val candidates = (1..20).map {
            Suggestion("Tab $it", "https://example.com/$it", SuggestionSource.HISTORY, recencyEpochMs = it.toLong())
        }
        assertEquals(8, SuggestionRanker.rank("example", candidates).size)
        assertEquals(3, SuggestionRanker.rank("example", candidates, limit = 3).size)
    }
}
