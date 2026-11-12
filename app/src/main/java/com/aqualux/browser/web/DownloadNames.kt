package com.aqualux.browser.web

import java.net.URI
import java.net.URLDecoder

object DownloadNames {
    fun fileName(contentDisposition: String?, url: String, mimeType: String?): String {
        fromDisposition(contentDisposition)?.let { return sanitize(it) }
        val path = try {
            URI(url).path.orEmpty()
        } catch (_: Exception) {
            url.substringBefore('?')
        }
        val last = path.substringAfterLast('/')
        if (last.isNotBlank()) {
            val decoded = decode(last)
            if (decoded.any { it.isLetterOrDigit() }) return sanitize(decoded)
        }
        return sanitize("download${extensionFor(mimeType)}")
    }

    fun fromDisposition(header: String?): String? {
        if (header.isNullOrBlank()) return null
        val star = Regex(
            """filename\*\s*=\s*(?:UTF-8''|utf-8'')?("[^"]+"|[^;]+)""",
            RegexOption.IGNORE_CASE,
        ).find(header)
        if (star != null) {
            val raw = star.groupValues[1].trim().trim('"')
            decode(raw).takeIf { it.isNotBlank() }?.let { return it }
        }
        val quoted = Regex("""filename\s*=\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(header)
        if (quoted != null) return quoted.groupValues[1].trim().takeIf { it.isNotBlank() }
        val plain = Regex("""filename\s*=\s*([^;]+)""", RegexOption.IGNORE_CASE).find(header)
        return plain?.groupValues?.get(1)?.trim()?.trim('"')?.takeIf { it.isNotBlank() }
    }

    fun sanitize(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\').trim()
        val cleaned = base.replace(Regex("""[^A-Za-z0-9._ -]"""), "_").trim(' ', '.', '_')
        return cleaned.take(120).ifBlank { "download" }
    }

    private fun decode(value: String): String = try {
        URLDecoder.decode(value, Charsets.UTF_8.name())
    } catch (_: Exception) {
        value
    }

    private fun extensionFor(mime: String?): String = when (
        mime?.substringBefore(';')?.trim()?.lowercase()
    ) {
        "application/pdf" -> ".pdf"
        "image/png" -> ".png"
        "image/jpeg" -> ".jpg"
        "image/gif" -> ".gif"
        "text/plain" -> ".txt"
        "text/html" -> ".html"
        "application/zip" -> ".zip"
        "application/vnd.android.package-archive" -> ".apk"
        else -> ""
    }
}
