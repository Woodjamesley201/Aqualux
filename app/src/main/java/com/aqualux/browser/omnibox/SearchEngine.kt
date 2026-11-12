package com.aqualux.browser.omnibox

enum class SearchEngine(
    val id: String,
    val label: String,
    val template: String,
) {
    GOOGLE("google", "Google", "https://www.google.com/search?q=%s"),
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    BING("bing", "Bing", "https://www.bing.com/search?q=%s"),
    BRAVE("brave", "Brave", "https://search.brave.com/search?q=%s"),
    ECOSIA("ecosia", "Ecosia", "https://www.ecosia.org/search?q=%s"),
    ;

    companion object {
        fun fromId(id: String?): SearchEngine = entries.firstOrNull { it.id == id } ?: GOOGLE
    }
}
