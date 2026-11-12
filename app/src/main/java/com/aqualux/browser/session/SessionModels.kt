package com.aqualux.browser.session

import kotlinx.serialization.Serializable

@Serializable
data class PersistedTab(
    val id: String,
    val url: String = "",
    val title: String = "",
    val scrollX: Int = 0,
    val scrollY: Int = 0,
    val pinned: Boolean = false,
    val desktop: Boolean = false,
    val faviconFile: String? = null,
)

@Serializable
data class PersistedSession(
    val version: Int = 1,
    val activeTabId: String? = null,
    val tabs: List<PersistedTab> = emptyList(),
)

@Serializable
data class HistoryEntry(
    val url: String,
    val title: String = "",
    val visitedAt: Long = 0L,
)

@Serializable
data class Bookmark(
    val id: String,
    val url: String,
    val title: String = "",
    val createdAt: Long = 0L,
)
