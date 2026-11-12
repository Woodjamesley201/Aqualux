package com.aqualux.browser.data

import android.content.Context
import android.graphics.Bitmap
import com.aqualux.browser.session.Bookmark
import com.aqualux.browser.session.HistoryEntry
import com.aqualux.browser.session.PersistedSession
import com.aqualux.browser.session.SessionSerializer
import java.io.File

class BrowserRepository(context: Context) {
    private val root = context.filesDir
    private val sessionFile = File(root, "session.json")
    private val historyFile = File(root, "history.json")
    private val bookmarksFile = File(root, "bookmarks.json")
    private val faviconDir = File(root, "favicons")
    private val lock = Any()

    fun readSession(): PersistedSession {
        synchronized(lock) {
            if (!sessionFile.exists()) return PersistedSession()
            return SessionSerializer.decodeOrNull(sessionFile.readText()) ?: PersistedSession()
        }
    }

    fun readHistory(): List<HistoryEntry> {
        synchronized(lock) {
            if (!historyFile.exists()) return emptyList()
            return try {
                SessionSerializer.decodeHistory(historyFile.readText())
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun readBookmarks(): List<Bookmark> {
        synchronized(lock) {
            if (!bookmarksFile.exists()) return emptyList()
            return try {
                SessionSerializer.decodeBookmarks(bookmarksFile.readText())
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun writeAll(session: PersistedSession, history: List<HistoryEntry>, bookmarks: List<Bookmark>) {
        synchronized(lock) {
            writeAtomic(sessionFile, SessionSerializer.encode(session))
            writeAtomic(historyFile, SessionSerializer.encodeHistory(history))
            writeAtomic(bookmarksFile, SessionSerializer.encodeBookmarks(bookmarks))
        }
    }

    fun saveFavicon(host: String, bitmap: Bitmap): String? {
        if (host.isBlank()) return null
        val safe = host.lowercase().replace(Regex("[^a-z0-9._-]"), "_").take(80).ifBlank { "host" }
        synchronized(lock) {
            faviconDir.mkdirs()
            val file = File(faviconDir, "$safe.png")
            val size = 64
            val scaled = if (bitmap.width == size && bitmap.height == size) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, size, size, true)
            }
            file.outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (scaled !== bitmap) scaled.recycle()
            return file.absolutePath
        }
    }

    private fun writeAtomic(file: File, text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }
}
