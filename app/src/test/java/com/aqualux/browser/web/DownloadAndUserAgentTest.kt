package com.aqualux.browser.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DownloadAndUserAgentTest {
    @Test
    fun dispositionAndUrlProduceASafeName() {
        assertEquals(
            "report.pdf",
            DownloadNames.fileName("attachment; filename=\"report.pdf\"", "https://example.com/x", "application/pdf"),
        )
        assertEquals(
            "quarterly report.pdf",
            DownloadNames.fileName(
                "attachment; filename*=UTF-8''quarterly%20report.pdf",
                "https://example.com/download",
                "application/pdf",
            ),
        )
        assertEquals(
            "archive.zip",
            DownloadNames.fileName(null, "https://cdn.example/files/archive.zip?token=1", "application/zip"),
        )
        assertEquals("download.pdf", DownloadNames.fileName(null, "https://example.com/", "application/pdf"))
        assertFalse(DownloadNames.sanitize("../../etc/passwd").contains("/"))
        assertEquals("download", DownloadNames.sanitize("   "))
    }

    @Test
    fun desktopUserAgentDropsTheMobileToken() {
        val mobile = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        val desktop = UserAgents.desktop(mobile)
        assertFalse(desktop.contains("Android"))
        assertFalse(desktop.contains("Mobile"))
        assertEquals(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            desktop,
        )
    }
}
