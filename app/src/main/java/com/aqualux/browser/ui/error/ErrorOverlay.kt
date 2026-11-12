package com.aqualux.browser.ui.error

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aqualux.browser.errors.ErrorKind
import com.aqualux.browser.errors.PageError
import com.aqualux.browser.ui.glass.GlassSurface
import com.aqualux.browser.ui.theme.LocalGlassTokens
import com.aqualux.browser.ui.theme.nestedRadius

@Composable
fun ErrorOverlay(error: PageError, url: String, onRetry: () -> Unit) {
    val tokens = LocalGlassTokens.current
    var details by rememberSaveable(error.code, error.kind) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = tokens.tabRowHeight + tokens.toolbarHeight + 36.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassSurface(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            shape = RoundedCornerShape(tokens.cardRadius),
            contentPadding = PaddingValues(22.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (error.kind) {
                        ErrorKind.SSL -> "Certificate"
                        ErrorKind.DNS -> "Address"
                        ErrorKind.CONNECTION -> "Connection"
                        ErrorKind.UNSAFE -> "Blocked"
                        ErrorKind.OTHER -> "Page"
                    },
                    color = tokens.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    error.title,
                    color = tokens.ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(error.message, color = tokens.muted, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
                if (url.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(url, color = tokens.ink, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
                }
                Spacer(Modifier.height(18.dp))
                GlassSurface(
                    modifier = Modifier.clickable(onClick = onRetry),
                    shape = RoundedCornerShape(tokens.nestedRadius(tokens.cardRadius, 8.dp)),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text("Try again", color = tokens.accent, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (details) "Hide details" else "Show details",
                    color = tokens.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { details = !details },
                )
                if (details) {
                    Spacer(Modifier.height(8.dp))
                    Text(error.technical, color = tokens.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
