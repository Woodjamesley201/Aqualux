package com.aqualux.browser.omnibox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniboxResolverTest {
    private val google = SearchEngine.GOOGLE

    @Test
    fun blankInputDoesNothing() {
        assertNull(OmniboxResolver.resolve("   ", google))
    }

    @Test
    fun explicitHttpsIsPreserved() {
        val target = OmniboxResolver.resolve("https://example.com/a", google)
        assertEquals(NavigationTarget.Web("https://example.com/a"), target)
    }

    @Test
    fun explicitHttpAndSchemeCaseAreNormalized() {
        val target = OmniboxResolver.resolve("HTTP://Example.COM/Path", google)
        assertEquals(NavigationTarget.Web("http://Example.COM/Path"), target)
    }

    @Test
    fun explicitHttpsLocalhostStaysHttps() {
        val target = OmniboxResolver.resolve("https://localhost:8443/x", google)
        assertEquals(NavigationTarget.Web("https://localhost:8443/x"), target)
    }

    @Test
    fun bareHostGetsHttps() {
        assertEquals(
            NavigationTarget.Web("https://example.com"),
            OmniboxResolver.resolve("  Example.COM  ", google),
        )
    }

    @Test
    fun bareHostKeepsPathAndQuery() {
        assertEquals(
            NavigationTarget.Web("https://example.com/path?q=1"),
            OmniboxResolver.resolve("example.com/path?q=1", google),
        )
    }

    @Test
    fun localhostAndIpUseHttp() {
        assertEquals(NavigationTarget.Web("http://localhost:3000/foo"), OmniboxResolver.resolve("localhost:3000/foo", google))
        assertEquals(NavigationTarget.Web("http://127.0.0.1:8080"), OmniboxResolver.resolve("127.0.0.1:8080", google))
        assertEquals(NavigationTarget.Web("http://192.168.0.1"), OmniboxResolver.resolve("192.168.0.1", google))
        assertEquals(NavigationTarget.Web("http://[::1]"), OmniboxResolver.resolve("[::1]", google))
    }

    @Test
    fun userInfoIsKeptOnBareHosts() {
        assertEquals(
            NavigationTarget.Web("https://user:pass@example.com"),
            OmniboxResolver.resolve("user:pass@example.com", google),
        )
    }

    @Test
    fun emailAndSpacesSearch() {
        val email = OmniboxResolver.resolve("name@gmail.com", google) as NavigationTarget.Search
        assertEquals("name@gmail.com", email.query)
        assertTrue(email.url.startsWith("https://www.google.com/search?q="))

        val spaced = OmniboxResolver.resolve("kotlin coroutines", google) as NavigationTarget.Search
        assertEquals("kotlin coroutines", spaced.query)
        assertTrue(spaced.url.contains("kotlin+coroutines"))
    }

    @Test
    fun punctuationQueryIsEncoded() {
        val target = OmniboxResolver.resolve("what is 2 + 2?", google) as NavigationTarget.Search
        assertTrue(target.url.contains("2+%2B+2%3F") || target.url.contains("what+is+2+%2B+2%3F"))
    }

    @Test
    fun searchEngineTemplateIsHonored() {
        val target = OmniboxResolver.resolve("tide tables", SearchEngine.DUCKDUCKGO) as NavigationTarget.Search
        assertTrue(target.url.startsWith("https://duckduckgo.com/?q="))
    }

    @Test
    fun dangerousSchemesAreBlocked() {
        assertTrue(OmniboxResolver.resolve("javascript:alert(1)", google) is NavigationTarget.Blocked)
        assertTrue(OmniboxResolver.resolve("file:///etc/passwd", google) is NavigationTarget.Blocked)
        assertTrue(OmniboxResolver.resolve("data:text/html,hi", google) is NavigationTarget.Blocked)
        assertTrue(OmniboxResolver.resolve("about:srcdoc", google) is NavigationTarget.Blocked)
    }

    @Test
    fun aboutBlankIsAllowed() {
        assertEquals(NavigationTarget.Web("about:blank"), OmniboxResolver.resolve("about:blank", google))
    }

    @Test
    fun incompleteAndFileLikeInputsDoNotBecomeUrls() {
        assertTrue(OmniboxResolver.resolve("https://", google) is NavigationTarget.Blocked)
        assertTrue(OmniboxResolver.resolve("notes.txt", google) is NavigationTarget.Search)
        assertTrue(OmniboxResolver.resolve("com", google) is NavigationTarget.Search)
        assertTrue(OmniboxResolver.resolve("a.b", google) is NavigationTarget.Search)
        assertTrue(OmniboxResolver.resolve("foo/bar", google) is NavigationTarget.Search)
    }

    @Test
    fun schemeRelativeHostGetsHttps() {
        assertEquals(
            NavigationTarget.Web("https://example.com/a"),
            OmniboxResolver.resolve("//example.com/a", google),
        )
    }

    @Test
    fun displayLabelStripsWww() {
        assertEquals("example.com", OmniboxResolver.displayLabel("https://www.example.com/a"))
        assertEquals("", OmniboxResolver.displayLabel(""))
    }
}
