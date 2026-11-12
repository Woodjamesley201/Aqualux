package com.aqualux.browser.omnibox

enum class SuggestionSource { TAB, BOOKMARK, HISTORY }

data class Suggestion(
    val title: String,
    val url: String,
    val source: SuggestionSource,
    val tabId: String? = null,
    val recencyEpochMs: Long = 0L,
)

object SuggestionRanker {
    fun rank(query: String, candidates: List<Suggestion>, limit: Int = 8): List<Suggestion> {
        val q = query.trim().lowercase()
        if (q.isEmpty() || limit <= 0) return emptyList()

        val ranked = candidates.mapNotNull { candidate ->
            val score = score(q, candidate)
            if (score == Int.MIN_VALUE) null else candidate to score
        }.sortedWith(
            compareByDescending<Pair<Suggestion, Int>> { it.second }
                .thenByDescending { it.first.recencyEpochMs },
        )

        val bestByUrl = LinkedHashMap<String, Suggestion>()
        for ((candidate, _) in ranked) {
            val key = candidate.url.lowercase()
            if (key !in bestByUrl) bestByUrl[key] = candidate
        }
        return bestByUrl.values.take(limit)
    }

    private fun score(query: String, candidate: Suggestion): Int {
        val title = candidate.title.lowercase()
        val url = candidate.url.lowercase()
        val host = OmniboxResolver.hostOf(candidate.url).removePrefix("www.")
        val bare = url.removePrefix("https://").removePrefix("http://")
        var score = when {
            host == query || bare.startsWith(query) -> 100
            title.startsWith(query) -> 82
            host.startsWith(query) -> 76
            url.contains(query) -> 58
            title.contains(query) -> 48
            else -> return Int.MIN_VALUE
        }
        score += when (candidate.source) {
            SuggestionSource.TAB -> 14
            SuggestionSource.BOOKMARK -> 8
            SuggestionSource.HISTORY -> 0
        }
        return score
    }
}
