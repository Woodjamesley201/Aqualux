package com.aqualux.browser.session

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSerializerTest {
    @Test
    fun roundTripPreservesOrderScrollPinsAndActiveTab() {
        val session = PersistedSession(
            version = 1,
            activeTabId = "b",
            tabs = listOf(
                PersistedTab(
                    id = "a",
                    url = "https://a.example/path",
                    title = "A",
                    scrollX = 0,
                    scrollY = 10,
                    pinned = false,
                ),
                PersistedTab(
                    id = "b",
                    url = "https://b.example",
                    title = "B",
                    scrollX = 3,
                    scrollY = 80,
                    pinned = true,
                    desktop = true,
                    faviconFile = "/tmp/b.png",
                ),
            ),
        )
        val again = SessionSerializer.decode(SessionSerializer.encode(session))
        assertEquals(session, again)
        assertEquals(listOf("a", "b"), again.tabs.map { it.id })
        assertEquals(80, again.tabs[1].scrollY)
        assertTrue(again.tabs[1].pinned)
        assertTrue(again.tabs[1].desktop)
    }

    @Test
    fun unknownFieldsAreIgnoredAndMissingFieldsDefault() {
        val raw = """
            {
              "version": 1,
              "activeTabId": "a",
              "extra": true,
              "tabs": [
                {"id": "a", "url": "https://example.com", "title": "Ex", "scrollX": 1, "scrollY": 40, "pinned": true, "future": 5}
              ]
            }
        """.trimIndent()
        val session = SessionSerializer.decode(raw)
        assertEquals("a", session.activeTabId)
        assertEquals(1, session.tabs.size)
        val tab = session.tabs.single()
        assertEquals("https://example.com", tab.url)
        assertEquals(40, tab.scrollY)
        assertTrue(tab.pinned)
        assertFalse(tab.desktop)
        assertNull(tab.faviconFile)
    }

    @Test
    fun sparseTabUsesDefaults() {
        val session = SessionSerializer.decode("""{"tabs":[{"id":"t"}]}""")
        val tab = session.tabs.single()
        assertEquals("t", tab.id)
        assertEquals("", tab.url)
        assertEquals("", tab.title)
        assertEquals(0, tab.scrollX)
        assertEquals(0, tab.scrollY)
        assertFalse(tab.pinned)
        assertNull(session.activeTabId)
    }

    @Test(expected = SerializationException::class)
    fun garbageIsRejected() {
        SessionSerializer.decode("not-json")
    }

    @Test
    fun historyAndBookmarksRoundTrip() {
        val history = listOf(HistoryEntry("https://example.com", "Example", 42L))
        val bookmarks = listOf(Bookmark("1", "https://example.com", "Example", 7L))
        assertEquals(history, SessionSerializer.decodeHistory(SessionSerializer.encodeHistory(history)))
        assertEquals(bookmarks, SessionSerializer.decodeBookmarks(SessionSerializer.encodeBookmarks(bookmarks)))
    }
}
