package com.aqualux.browser.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aqualux.browser.omnibox.OmniboxResolver
import com.aqualux.browser.session.Bookmark
import com.aqualux.browser.ui.glass.GlassIconButton
import com.aqualux.browser.ui.theme.LocalGlassTokens

@Composable
fun BookmarksPage(
    bookmarks: List<Bookmark>,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalGlassTokens.current
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            Text("Bookmarks", color = tokens.ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }
        if (bookmarks.isEmpty()) {
            Text(
                "Pages you bookmark show up here, and on the new-tab screen.",
                color = tokens.muted,
                modifier = Modifier.padding(20.dp),
                fontSize = 15.sp,
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(bookmark.url) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                bookmark.title.ifBlank { bookmark.url },
                                color = tokens.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                OmniboxResolver.displayLabel(bookmark.url),
                                color = tokens.muted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        GlassIconButton(Icons.Filled.Delete, "Delete bookmark", { onDelete(bookmark.id) })
                    }
                }
            }
        }
    }
}
