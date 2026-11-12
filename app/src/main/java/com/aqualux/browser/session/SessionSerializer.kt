package com.aqualux.browser.session

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SessionSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        isLenient = true
        explicitNulls = false
    }

    fun encode(session: PersistedSession): String = json.encodeToString(session)

    fun decode(text: String): PersistedSession = json.decodeFromString(text)

    fun encodeHistory(entries: List<HistoryEntry>): String = json.encodeToString(entries)

    fun decodeHistory(text: String): List<HistoryEntry> = json.decodeFromString(text)

    fun encodeBookmarks(entries: List<Bookmark>): String = json.encodeToString(entries)

    fun decodeBookmarks(text: String): List<Bookmark> = json.decodeFromString(text)

    fun decodeOrNull(text: String): PersistedSession? = try {
        decode(text)
    } catch (_: SerializationException) {
        null
    }
}
