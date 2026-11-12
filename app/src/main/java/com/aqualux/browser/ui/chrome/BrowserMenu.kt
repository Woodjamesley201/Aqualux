package com.aqualux.browser.ui.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aqualux.browser.ui.glass.GlassSurface
import com.aqualux.browser.ui.glass.GlassVariant
import com.aqualux.browser.ui.icons.BrowserIcons
import com.aqualux.browser.ui.theme.LocalGlassTokens

@Composable
fun BrowserMenu(
    bookmarked: Boolean,
    desktop: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onDuplicate: () -> Unit,
    onFind: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onDesktop: () -> Unit,
    onSettings: () -> Unit,
) {
    val tokens = LocalGlassTokens.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val top = with(density) { (tokens.shelfMargin * 2 + tokens.shelfPad * 2 + tokens.tabRowHeight + tokens.rowGap + tokens.toolbarHeight + 8.dp).roundToPx() }
    val maxHeight = (configuration.screenHeightDp * 0.7f).dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(tokens.scrim)
                .clickable(onClick = onDismiss),
        ) {
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = with(density) { top.toDp() }, end = 16.dp)
                    .widthIn(max = (configuration.screenWidthDp - 32).dp)
                    .width(320.dp)
                    .heightIn(max = maxHeight)
                    .clickable(enabled = true, onClick = {}),
                shape = RoundedCornerShape(28.dp),
                variant = GlassVariant.Popover,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            ) {
                Column {
                    Column(
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        MenuGroup {
                            MenuRow("New tab", Icons.Filled.Add, onNewTab)
                            MenuRow("Duplicate tab", BrowserIcons.Tabs, onDuplicate)
                        }
                        Hairline()
                        MenuGroup {
                            MenuRow("Find in page", Icons.Filled.Search, onFind)
                            MenuRow("Share", Icons.Filled.Share, onShare)
                            MenuRow(
                                if (bookmarked) "Remove bookmark" else "Bookmark this page",
                                if (bookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                                onBookmark,
                            )
                        }
                        Hairline()
                        MenuGroup {
                            MenuRow("Bookmarks", BrowserIcons.Bookmark, onBookmarks)
                            MenuRow("Downloads", BrowserIcons.Download, onDownloads)
                            MenuRow(
                                if (desktop) "Request mobile site" else "Request desktop site",
                                BrowserIcons.Desktop,
                                onDesktop,
                            )
                        }
                    }
                    Hairline()
                    MenuRow("Settings", Icons.Filled.Settings, onSettings)
                }
            }
        }
    }
}

@Composable
private fun MenuGroup(content: @Composable () -> Unit) {
    Column(content = { content() })
}

@Composable
private fun Hairline() {
    val tokens = LocalGlassTokens.current
    Box(
        Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(tokens.hairlineOuter),
    )
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    val tokens = LocalGlassTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tokens.ink, modifier = Modifier.height(20.dp).width(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = tokens.ink, fontSize = 15.sp)
    }
}
