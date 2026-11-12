package com.aqualux.browser.ui.glass

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import com.aqualux.browser.ui.theme.GlassTokens
import com.aqualux.browser.ui.theme.LocalGlassTokens
import com.aqualux.browser.ui.theme.LocalReduceMotion

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalGlassTokens.current
    val reduceMotion = LocalReduceMotion.current
    var press by remember { mutableStateOf<Offset?>(null) }
    var lastPress by remember { mutableStateOf(Offset.Zero) }
    val highlight by animateFloatAsState(
        targetValue = if (press != null) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = if (press != null) 90 else 260),
        label = "glass-touch",
    )
    val blurPx = with(androidx.compose.ui.platform.LocalDensity.current) { tokens.blurRadius.toPx() }
    GlassSurfaceBody(
        modifier = modifier,
        shape = shape,
        contentPadding = contentPadding,
        tokens = tokens,
        highlight = highlight,
        lastPress = lastPress,
        onPress = {
            lastPress = it
            press = it
        },
        onRelease = { press = null },
        blurPx = blurPx,
        content = content,
    )
}

@Composable
private fun GlassSurfaceBody(
    modifier: Modifier,
    shape: Shape,
    contentPadding: PaddingValues,
    tokens: GlassTokens,
    highlight: Float,
    lastPress: Offset,
    onPress: (Offset) -> Unit,
    onRelease: () -> Unit,
    blurPx: Float,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = tokens.ambientElevation,
                shape = shape,
                clip = false,
                ambientColor = tokens.ambientShadow,
                spotColor = tokens.ambientShadow,
            )
            .clip(shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    onPress(down.position)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                    }
                    onRelease()
                }
            },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx > 0.5f) {
                        renderEffect = BlurEffect(blurPx, blurPx, TileMode.Clamp)
                    }
                }
                .drawBehind { drawGlassFill(tokens) },
        )
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawGlassEdge(tokens, shape, highlight, lastPress)
                },
        )
        Box(Modifier.padding(contentPadding), content = content)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlassFill(tokens: GlassTokens) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to tokens.fillTop,
            0.42f to tokens.fillMid,
            1f to tokens.fillBottom,
        ),
    )
    drawCircle(
        color = tokens.accent.copy(alpha = if (tokens.dark) 0.08f else 0.06f),
        radius = size.minDimension * 0.7f,
        center = Offset(size.width * 0.12f, 0f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlassEdge(
    tokens: GlassTokens,
    shape: Shape,
    highlight: Float,
    touch: Offset,
) {
    if (tokens.refraction && highlight > 0.01f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tokens.refractHighlight.copy(alpha = tokens.refractHighlight.alpha * highlight),
                    Color.Transparent,
                ),
                center = touch,
                radius = 140.dp.toPx(),
            ),
            radius = 140.dp.toPx(),
            center = touch,
        )
    }
    val outline = shape.createOutline(size, layoutDirection, this)
    val hairline = Stroke(width = 1.dp.toPx())
    when (outline) {
        is Outline.Rounded -> drawPath(
            path = androidx.compose.ui.graphics.Path().apply { addRoundRect(outline.roundRect) },
            color = tokens.hairline,
            style = hairline,
        )
        is Outline.Rectangle -> drawRect(color = tokens.hairline, style = hairline)
        is Outline.Generic -> drawPath(path = outline.path, color = tokens.hairline, style = hairline)
    }
    val y = 0.5.dp.toPx()
    drawLine(
        color = tokens.specular,
        start = Offset(size.width * 0.06f, y),
        end = Offset(size.width * 0.94f, y),
        strokeWidth = 1.dp.toPx(),
    )
    drawRect(
        brush = Brush.verticalGradient(
            0f to tokens.innerGlow.copy(alpha = tokens.innerGlow.alpha * 0.35f),
            0.2f to Color.Transparent,
            0.82f to Color.Transparent,
            1f to tokens.innerShadow,
        ),
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalGlassTokens.current.ink,
    size: Dp = LocalGlassTokens.current.toolbarHeight,
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, color = tokens.accent),
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else tokens.muted.copy(alpha = 0.45f),
            modifier = Modifier.size(tokens.iconSize),
        )
    }
}

@Composable
fun LetterMark(label: String, size: Dp, modifier: Modifier = Modifier) {
    val tokens = LocalGlassTokens.current
    val letter = label.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tokens.accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = tokens.accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.42f).sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun GlassToast(message: String, modifier: Modifier = Modifier) {
    val tokens = LocalGlassTokens.current
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        GlassSurface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.cardRadius),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides tokens.ink) {
                Text(message, color = tokens.ink, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun AquaMark(modifier: Modifier = Modifier, size: Dp = 72.dp) {
    val tokens = LocalGlassTokens.current
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f, h * 0.06f)
                cubicTo(w * 0.5f, h * 0.06f, w * 0.12f, h * 0.42f, w * 0.12f, h * 0.62f)
                cubicTo(w * 0.12f, h * 0.86f, w * 0.28f, h * 0.98f, w * 0.5f, h * 0.98f)
                cubicTo(w * 0.72f, h * 0.98f, w * 0.88f, h * 0.86f, w * 0.88f, h * 0.62f)
                cubicTo(w * 0.88f, h * 0.42f, w * 0.5f, h * 0.06f, w * 0.5f, h * 0.06f)
                close()
            }
            drawPath(path, color = if (tokens.dark) Color(0xFFE7FFFB) else Color.White)
            drawCircle(
                color = tokens.accent.copy(alpha = 0.85f),
                radius = w * 0.12f,
                center = Offset(w * 0.42f, h * 0.62f),
            )
            drawCircle(
                color = if (tokens.dark) Color(0xFF08343C) else tokens.accent,
                radius = w * 0.045f,
                center = Offset(w * 0.5f, h * 0.74f),
            )
        }
    }
}
