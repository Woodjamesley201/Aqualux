package com.aqualux.browser.errors

import com.aqualux.browser.security.WebSecurityPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorTaxonomyTest {
    @Test
    fun classifiesDnsConnectionSslAndUnsafe() {
        assertEquals(ErrorKind.DNS, ErrorTaxonomy.fromWebView(ErrorTaxonomy.ERROR_HOST_LOOKUP, "dns", "example.com").kind)
        assertEquals(ErrorKind.CONNECTION, ErrorTaxonomy.fromWebView(ErrorTaxonomy.ERROR_CONNECT, "refused", "example.com").kind)
        assertEquals(ErrorKind.CONNECTION, ErrorTaxonomy.fromWebView(ErrorTaxonomy.ERROR_TIMEOUT, "timeout", "example.com").kind)
        assertEquals(ErrorKind.SSL, ErrorTaxonomy.fromWebView(ErrorTaxonomy.ERROR_FAILED_SSL_HANDSHAKE, "handshake", "example.com").kind)
        assertEquals(ErrorKind.UNSAFE, ErrorTaxonomy.fromWebView(ErrorTaxonomy.ERROR_UNSAFE_RESOURCE, "malware", "example.com").kind)
    }

    @Test
    fun sslReasonsStaySpecificAndDoNotOfferABypass() {
        val expired = ErrorTaxonomy.fromSsl(ErrorTaxonomy.SSL_EXPIRED, "example.com")
        val mismatch = ErrorTaxonomy.fromSsl(ErrorTaxonomy.SSL_IDMISMATCH, "example.com")
        assertEquals(ErrorKind.SSL, expired.kind)
        assertTrue(expired.message.contains("expired", ignoreCase = true))
        assertTrue(mismatch.message.contains("doesn't match", ignoreCase = true))
        assertFalse(expired.message.contains("proceed", ignoreCase = true))
        assertFalse(WebSecurityPolicy.sslProceedAllowed)
    }

    @Test
    fun securityPolicyStaysClosed() {
        assertFalse(WebSecurityPolicy.javascriptInterfaceAllowed)
        assertFalse(WebSecurityPolicy.allowFileAccess)
        assertFalse(WebSecurityPolicy.allowContentAccess)
        assertFalse(WebSecurityPolicy.allowFileAccessFromFileUrls)
        assertFalse(WebSecurityPolicy.allowUniversalAccessFromFileUrls)
        assertTrue(WebSecurityPolicy.safeBrowsing)
        assertEquals("never", WebSecurityPolicy.mixedContent)
    }
}
