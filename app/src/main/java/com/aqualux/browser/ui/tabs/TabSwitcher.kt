package com.aqualux.browser.ui.tabs

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aqualux.browser.model.TabSnapshot
import com.aqualux.browser.omnibox.OmniboxResolver
import com.aqualux.browser.ui.glass.GlassIconButton
import com.aqualux.browser.ui.glass.GlassSurface
import com.aqualux.browser.ui.glass.LetterMark
import com.aqualux.browser.ui.icons.BrowserIcons
import com.aqualux.browser.ui.theme.LocalGlassTokens
import com.aqualux.browser.ui.theme.LocalReduceMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun TabSwitcher(
    tabs: List<TabSnapshot>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNewTab: () -> Unit,
    onPin: (String, Boolean) -> Unit,
    onDuplicate: (String) -> Unit,
    onMove: (String, String) -> Unit,
    onDone: () -> Unit,
) {
    val tokens = LocalGlassTokens.current
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 320f))
        }
    }
    val shiftPx = with(LocalDensity.current) { 28.dp.toPx() }
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var lastTarget by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(tokens.scrim.copy(alpha = tokens.scrim.alpha * progress.value.coerceIn(0f, 1f)))
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Tabs",
                    color = tokens.ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                GlassIconButton(Icons.Filled.Add, "New tab", onNewTab)
                GlassIconButton(Icons.Filled.Close, "Done", onDone)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Long-press a card to reorder it. Pinned tabs stay with other pinned tabs.",
                color = tokens.muted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(tabs, key = { it.id }) { tab ->
                    val dragging = draggingId == tab.id
                    val appear = if (tab.id == activeId) 1f else progress.value
                    TabCard(
                        tab = tab,
                        selected = tab.id == activeId,
                        modifier = Modifier
                            .onGloballyPositioned { bounds[tab.id] = it.boundsInRoot() }
                            .zIndex(if (dragging) 2f else 0f)
                            .graphicsLayer {
                                alpha = if (dragging) 0.96f else appear.coerceIn(0f, 1f)
                                val lift = if (tab.id == activeId) {
                                    androidx.compose.ui.util.lerp(1.18f, 1f, progress.value)
                                } else {
                                    1f
                                }
                                scaleX = if (dragging) 1.04f else lift
                                scaleY = if (dragging) 1.04f else lift
                                if (dragging) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                } else if (tab.id != activeId) {
                                    translationY = (1f - progress.value) * shiftPx
                                }
                            }
                            .pointerInput(tab.id, tab.pinned) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = tab.id
                                        dragOffset = Offset.Zero
                                        lastTarget = null
                                    },
                                    onDragEnd = {
                                        draggingId = null
                                        dragOffset = Offset.Zero
                                        lastTarget = null
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragOffset = Offset.Zero
                                        lastTarget = null
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount
                                        val origin = bounds[tab.id] ?: return@detectDragGesturesAfterLongPress
                                        val finger = origin.center + dragOffset
                                        val target = bounds.entries.firstOrNull { (id, rect) ->
                                            id != tab.id && rect.contains(finger)
                                        }?.key
                                        if (target != null && target != lastTarget) {
                                            lastTarget = target
                                            onMove(tab.id, target)
                                        }
                                    },
                                )
                            },
                        onSelect = { onSelect(tab.id) },
                        onClose = { onClose(tab.id) },
                        onPin = { onPin(tab.id, !tab.pinned) },
                        onDuplicate = { onDuplicate(tab.id) },
                    )
                }
            }
        }
        val ghostId = activeId
        val ghostBounds = ghostId?.let { bounds[it] }
        if (!reduceMotion && ghostBounds != null && progress.value < 0.98f) {
            val root = bounds.values.fold(ghostBounds) { acc, rect ->
                Rect(
                    minOf(acc.left, rect.left),
                    minOf(acc.top, rect.top),
                    maxOf(acc.right, rect.right),
                    maxOf(acc.bottom, rect.bottom),
                )
            }
            val full = Rect(0f, 0f, root.right.coerceAtLeast(ghostBounds.right), root.bottom.coerceAtLeast(ghostBounds.bottom))
            val rect = androidx.compose.ui.geometry.lerp(full, ghostBounds, progress.value)
            val ghostDensity = LocalDensity.current
            Box(
                Modifier
                    .graphicsLayer {
                        translationX = rect.left
                        translationY = rect.top
                    }
                    .size(
                        width = with(ghostDensity) { rect.width.toDp() },
                        height = with(ghostDensity) { rect.height.toDp() },
                    )
                    .background(tokens.pageTop, RoundedCornerShape(tokens.cardRadius * progress.value)),
            )
        }
    }
}

@Composable
private fun TabCard(
    tab: TabSnapshot,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onPin: () -> Unit,
    onDuplicate: () -> Unit,
) {
    val tokens = LocalGlassTokens.current
    val favicon by produceState<ImageBitmap?>(initialValue = null, tab.faviconFile) {
        value = withContext(Dispatchers.IO) {
            val path = tab.faviconFile ?: return@withContext null
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(tokens.cardRadius),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .background(
                        if (selected) tokens.accentSoft else tokens.hoverFill,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (favicon != null) {
                    Image(favicon!!, null, Modifier.size(42.dp))
                } else {
                    LetterMark(tab.title.ifBlank { tab.url }, 48.dp)
                }
                if (tab.isLoading) {
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(tab.progress.coerceIn(0.05f, 1f))
                            .height(3.dp)
                            .background(tokens.accent),
                    )
                }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    tab.title.ifBlank { "New tab" },
                    color = tokens.ink,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                )
                Text(
                    when {
                        tab.unloaded && tab.url.isNotBlank() -> "Sleeping · ${OmniboxResolver.displayLabel(tab.url)}"
                        tab.url.isBlank() -> "New tab"
                        else -> OmniboxResolver.displayLabel(tab.url)
                    },
                    color = tokens.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(
                        icon = BrowserIcons.Pin,
                        contentDescription = if (tab.pinned) "Unpin" else "Pin",
                        onClick = onPin,
                        size = 32.dp,
                        tint = if (tab.pinned) tokens.accent else tokens.muted,
                    )
                    GlassIconButton(Icons.Filled.Star, "Duplicate", onDuplicate, size = 32.dp, tint = tokens.muted)
                    Spacer(Modifier.weight(1f))
                    GlassIconButton(Icons.Filled.Close, "Close tab", onClose, size = 32.dp)
                }
            }
        }
    }
}
