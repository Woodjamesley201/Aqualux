package com.aqualux.browser.omnibox

import java.net.IDN
import java.net.URI
import java.net.URLEncoder

sealed interface NavigationTarget {
    data class Web(val url: String) : NavigationTarget
    data class Search(val query: String, val url: String) : NavigationTarget
    data class Blocked(val reason: String) : NavigationTarget
}

/**
 * Turns omnibox text into a navigation. Explicit http(s) is preserved.
 * A bare public host is prefixed with https. Local names and IP addresses
 * are prefixed with http, because that is what they almost always speak.
 * javascript:, file:, and data: are refused.
 */
object OmniboxResolver {
    private val fileLikeTlds = setOf(
        "txt", "pdf", "png", "jpg", "jpeg", "gif", "webp", "svg", "json", "xml",
        "html", "htm", "zip", "apk", "mp4", "mp3", "wav", "csv", "md", "js", "css",
    )

    fun resolve(rawInput: String, engine: SearchEngine): NavigationTarget? {
        val raw = rawInput.trim()
        if (raw.isEmpty()) return null
        if (raw.any { it.isISOControl() }) {
            return NavigationTarget.Blocked("That address can't be opened.")
        }

        val schemeSep = raw.indexOf("://")
        if (schemeSep >= 0) {
            val scheme = raw.substring(0, schemeSep).lowercase()
            return when (scheme) {
                "http", "https" -> normalizeAbsolute(raw)
                else -> NavigationTarget.Blocked("Aqualux only opens http and https addresses.")
            }
        }

        if (raw.startsWith("about:", ignoreCase = true)) {
            return if (raw.equals("about:blank", ignoreCase = true)) {
                NavigationTarget.Web("about:blank")
            } else {
                NavigationTarget.Blocked("Only about:blank is allowed.")
            }
        }

        if (raw.startsWith("//")) {
            return normalizeAbsolute("https:$raw")
        }

        if (raw.any { it.isWhitespace() }) {
            return search(raw, engine)
        }

        if (looksLikeEmail(raw)) {
            return search(raw, engine)
        }

        if (isUnsafeScheme(raw)) {
            return NavigationTarget.Blocked("Aqualux only opens http and https addresses.")
        }

        bareWebUrl(raw)?.let { return NavigationTarget.Web(it) }
        return search(raw, engine)
    }

    fun searchUrl(engine: SearchEngine, query: String): String {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        return engine.template.replace("%s", encoded)
    }

    fun hostOf(url: String): String {
        if (url.isBlank()) return ""
        return try {
            URI(url).host?.lowercase().orEmpty()
        } catch (_: Exception) {
            url.substringAfter("://", url)
                .substringBefore("/")
                .substringBefore("?")
                .substringBefore("#")
                .substringAfter("@")
                .substringBefore(":")
                .lowercase()
        }
    }

    fun displayLabel(url: String): String {
        if (url.isBlank()) return ""
        if (url.equals("about:blank", ignoreCase = true)) return "about:blank"
        val host = hostOf(url).removePrefix("www.")
        return host.ifBlank { url }
    }

    private fun search(query: String, engine: SearchEngine): NavigationTarget.Search {
        return NavigationTarget.Search(query = query, url = searchUrl(engine, query))
    }

    /**
     * Non-http schemes typed into the omnibox. Ports (`localhost:3000`) and
     * userinfo (`user:pass@host`) are not schemes, so they fall through.
     */
    private fun isUnsafeScheme(raw: String): Boolean {
        val colon = raw.indexOf(':')
        if (colon <= 0) return false
        val scheme = raw.substring(0, colon).lowercase()
        return scheme in blockedSchemes
    }

    private val blockedSchemes = setOf(
        "javascript", "data", "file", "blob", "content", "intent", "ftp",
        "mailto", "tel", "sms", "smsto", "geo", "market", "chrome", "about",
    )

    private fun looksLikeEmail(raw: String): Boolean {
        val at = raw.indexOf('@')
        if (at <= 0 || at != raw.lastIndexOf('@')) return false
        if (raw.contains('/') || raw.contains(':')) return false
        val domain = raw.substring(at + 1)
        return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
    }

    private fun normalizeAbsolute(raw: String): NavigationTarget {
        val match = Regex("""(?i)^(https?)://(\S+)$""").find(raw.trim())
            ?: return NavigationTarget.Blocked("That address can't be opened.")
        val scheme = match.groupValues[1].lowercase()
        val rest = match.groupValues[2]
        if (rest.isBlank() || rest.startsWith("/")) {
            return NavigationTarget.Blocked("That address is incomplete.")
        }
        val normalized = "$scheme://$rest"
        return try {
            val host = URI(normalized).host
            if (host.isNullOrBlank()) {
                NavigationTarget.Blocked("That address is incomplete.")
            } else {
                NavigationTarget.Web(normalized)
            }
        } catch (_: Exception) {
            val hostPart = rest.substringBefore('/').substringBefore('?').substringBefore('#')
            if (hostPart.isBlank()) {
                NavigationTarget.Blocked("That address is incomplete.")
            } else {
                NavigationTarget.Web(normalized)
            }
        }
    }

    private fun bareWebUrl(input: String): String? {
        val cut = input.indexOfAny(charArrayOf('/', '?', '#'))
        val authority = if (cut == -1) input else input.substring(0, cut)
        val suffix = if (cut == -1) "" else input.substring(cut)
        if (authority.isBlank()) return null

        val (userInfo, hostPort) = splitUserInfo(authority) ?: return null
        val (hostRaw, port) = splitHostPort(hostPort) ?: return null
        if (port != null && port !in 1..65535) return null

        val host = hostRaw.trim().trimEnd('.')
        if (host.isEmpty()) return null

        val local = isLocalHost(host)
        val ip = isIp(host)
        if (!local && !ip && !isDomain(host)) return null
        if (!local && !ip && isFileLike(host) && suffix.isEmpty()) return null

        val scheme = if (local || ip) "http" else "https"
        val asciiHost = toAsciiHost(host) ?: return null
        val portPart = if (port != null) ":$port" else ""
        val userPart = if (userInfo != null) "$userInfo@" else ""
        return "$scheme://$userPart$asciiHost$portPart$suffix"
    }

    private fun splitUserInfo(authority: String): Pair<String?, String>? {
        val at = authority.lastIndexOf('@')
        if (at < 0) return null to authority
        if (at == 0) return null
        val info = authority.substring(0, at)
        if (info.isBlank() || info.any { it.isWhitespace() }) return null
        return info to authority.substring(at + 1)
    }

    private fun splitHostPort(hostPort: String): Pair<String, Int?>? {
        if (hostPort.startsWith("[")) {
            val end = hostPort.indexOf(']')
            if (end < 0) return null
            val host = hostPort.substring(0, end + 1)
            val rest = hostPort.substring(end + 1)
            if (rest.isEmpty()) return host to null
            if (!rest.startsWith(":")) return null
            val port = rest.substring(1).toIntOrNull() ?: return null
            return host to port
        }
        val colon = hostPort.lastIndexOf(':')
        if (colon > 0 && hostPort.indexOf(':') == colon) {
            val host = hostPort.substring(0, colon)
            val port = hostPort.substring(colon + 1).toIntOrNull() ?: return null
            return host to port
        }
        if (hostPort.count { it == ':' } > 1) return null
        return hostPort to null
    }

    private fun isLocalHost(host: String): Boolean {
        val h = host.lowercase()
        return h == "localhost" || h.endsWith(".localhost") || h.endsWith(".local")
    }

    private fun isIp(host: String): Boolean {
        if (host.startsWith("[") && host.endsWith("]")) {
            val inner = host.substring(1, host.length - 1)
            return inner.isNotEmpty() && inner.all { ch ->
                ch.isDigit() || ch in 'a'..'f' || ch in 'A'..'F' || ch == ':' || ch == '.'
            }
        }
        val parts = host.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            val value = part.toIntOrNull() ?: return false
            value in 0..255 && (part.length == 1 || !part.startsWith('0'))
        }
    }

    private fun isDomain(host: String): Boolean {
        if (host.length > 253 || host.length < 3) return false
        val labels = host.split('.')
        if (labels.size < 2) return false
        val tld = labels.last()
        if (tld.length < 2 || !tld.all { it.isLetter() }) return false
        return labels.all { label ->
            label.isNotEmpty() &&
                label.length <= 63 &&
                !label.startsWith('-') &&
                !label.endsWith('-') &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }

    private fun isFileLike(host: String): Boolean {
        return host.substringAfterLast('.').lowercase() in fileLikeTlds
    }

    private fun toAsciiHost(host: String): String? {
        if (host.startsWith("[")) return host.lowercase()
        return try {
            IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).lowercase()
        } catch (_: Exception) {
            null
        }
    }
}
