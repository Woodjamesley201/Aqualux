package com.aqualux.browser.errors

enum class ErrorKind { SSL, DNS, CONNECTION, UNSAFE, OTHER }

data class PageError(
    val kind: ErrorKind,
    val code: Int,
    val title: String,
    val message: String,
    val technical: String,
)

/**
 * WebView and SslError integer codes, kept here so classification can be
 * tested without the Android framework. The values match the platform constants.
 */
object ErrorTaxonomy {
    const val ERROR_UNKNOWN = -1
    const val ERROR_HOST_LOOKUP = -2
    const val ERROR_UNSUPPORTED_AUTH_SCHEME = -3
    const val ERROR_AUTHENTICATION = -4
    const val ERROR_PROXY_AUTHENTICATION = -5
    const val ERROR_CONNECT = -6
    const val ERROR_IO = -7
    const val ERROR_TIMEOUT = -8
    const val ERROR_REDIRECT_LOOP = -9
    const val ERROR_UNSUPPORTED_SCHEME = -10
    const val ERROR_FAILED_SSL_HANDSHAKE = -11
    const val ERROR_BAD_URL = -12
    const val ERROR_FILE = -13
    const val ERROR_FILE_NOT_FOUND = -14
    const val ERROR_TOO_MANY_REQUESTS = -15
    const val ERROR_UNSAFE_RESOURCE = -16

    const val SSL_NOTYETVALID = 0
    const val SSL_EXPIRED = 1
    const val SSL_IDMISMATCH = 2
    const val SSL_UNTRUSTED = 3
    const val SSL_DATE_INVALID = 4
    const val SSL_INVALID = 5

    fun fromWebView(code: Int, description: String?, host: String): PageError {
        val site = host.ifBlank { "this site" }
        val detail = description?.takeIf { it.isNotBlank() } ?: "Error $code"
        return when (code) {
            ERROR_HOST_LOOKUP -> PageError(
                kind = ErrorKind.DNS,
                code = code,
                title = "Can't find this site",
                message = "The address for $site couldn't be resolved. Check the spelling, or try again when the network is steadier.",
                technical = detail,
            )
            ERROR_CONNECT, ERROR_TIMEOUT, ERROR_IO, ERROR_PROXY_AUTHENTICATION -> PageError(
                kind = ErrorKind.CONNECTION,
                code = code,
                title = "This site can't be reached",
                message = "The connection to $site failed or timed out. The site may be down, or the network may be blocking it.",
                technical = detail,
            )
            ERROR_FAILED_SSL_HANDSHAKE -> fromSsl(SSL_INVALID, host).copy(
                code = code,
                technical = detail,
            )
            ERROR_UNSAFE_RESOURCE -> unsafe(host).copy(code = code, technical = detail)
            else -> PageError(
                kind = ErrorKind.OTHER,
                code = code,
                title = "This page didn't load",
                message = "Aqualux couldn't open $site.",
                technical = detail,
            )
        }
    }

    fun fromSsl(primaryError: Int, host: String): PageError {
        val site = host.ifBlank { "this site" }
        val reason = when (primaryError) {
            SSL_EXPIRED -> "The certificate has expired."
            SSL_IDMISMATCH -> "The certificate doesn't match $site."
            SSL_UNTRUSTED -> "The certificate isn't trusted."
            SSL_DATE_INVALID -> "The certificate date is invalid."
            SSL_NOTYETVALID -> "The certificate isn't valid yet."
            else -> "The certificate couldn't be verified."
        }
        return PageError(
            kind = ErrorKind.SSL,
            code = primaryError,
            title = "This connection isn't private",
            message = "Aqualux couldn't verify the certificate for $site, so the page was not opened. $reason",
            technical = "SSL error $primaryError: $reason",
        )
    }

    fun unsafe(host: String): PageError {
        val site = host.ifBlank { "This site" }
        return PageError(
            kind = ErrorKind.UNSAFE,
            code = ERROR_UNSAFE_RESOURCE,
            title = "Dangerous site blocked",
            message = "Safe Browsing flagged $site. Aqualux did not open it.",
            technical = "Safe Browsing threat",
        )
    }

    fun renderer(host: String): PageError {
        val site = host.ifBlank { "The page" }
        return PageError(
            kind = ErrorKind.OTHER,
            code = ERROR_UNKNOWN,
            title = "The page process stopped",
            message = "$site stopped unexpectedly. You can try loading it again.",
            technical = "Renderer process gone",
        )
    }
}
