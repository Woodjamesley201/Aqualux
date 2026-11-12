package com.aqualux.browser.ui.start

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aqualux.browser.omnibox.OmniboxResolver
import com.aqualux.browser.session.Bookmark
import com.aqualux.browser.session.HistoryEntry
import com.aqualux.browser.ui.glass.AquaMark
import com.aqualux.browser.ui.glass.GlassSurface
import com.aqualux.browser.ui.theme.LocalGlassTokens

@Composable
fun StartPage(
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    onSearch: () -> Unit,
    onOpen: (String) -> Unit,
    message: String? = null,
) {
    val tokens = LocalGlassTokens.current
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = tokens.tabRowHeight + tokens.toolbarHeight + tokens.shelfMargin + 28.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        AquaMark(size = 84.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Aqualux",
            color = tokens.ink,
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp,
        )
        Text(
            "A quieter way to browse",
            color = tokens.muted,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(28.dp))
        GlassSurface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clickable(onClick = onSearch),
            shape = RoundedCornerShape(tokens.omniboxHeight / 2),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, null, tint = tokens.muted)
                Text(
                    "Search or type a URL",
                    color = tokens.muted,
                    modifier = Modifier.padding(start = 12.dp),
                    fontSize = 16.sp,
                )
            }
        }
        if (message != null) {
            Spacer(Modifier.height(16.dp))
            Text(message, color = tokens.danger, textAlign = TextAlign.Center)
        }
        if (bookmarks.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            SectionLabel("Bookmarks")
            bookmarks.take(8).forEach { bookmark ->
                LinkRow(bookmark.title.ifBlank { bookmark.url }, bookmark.url) { onOpen(bookmark.url) }
            }
        }
        if (history.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionLabel("Recent")
            history.take(6).forEach { entry ->
                LinkRow(entry.title.ifBlank { entry.url }, entry.url) { onOpen(entry.url) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val tokens = LocalGlassTokens.current
    Text(
        text,
        color = tokens.muted,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(bottom = 6.dp, start = 4.dp),
    )
}

@Composable
private fun LinkRow(title: String, url: String, onClick: () -> Unit) {
    val tokens = LocalGlassTokens.current
    Column(
        Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, color = tokens.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
        Text(
            OmniboxResolver.displayLabel(url),
            color = tokens.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
        )
    }
}
