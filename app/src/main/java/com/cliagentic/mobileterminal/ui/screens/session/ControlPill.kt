package com.cliagentic.mobileterminal.ui.screens.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cliagentic.mobileterminal.data.model.TerminalSkin
import com.cliagentic.mobileterminal.ui.theme.JetBrainsMono

@Composable
internal fun ControlPill(
    isConnected: Boolean,
    isConnecting: Boolean,
    terminalSkin: TerminalSkin,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Surface(
            onClick = {
                onTap()
            },
            shape = MaterialTheme.shapes.large,
            color = terminalSkin.selection.copy(alpha = 0.85f),
            contentColor = terminalSkin.foreground
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isConnecting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            color = terminalSkin.accent
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isConnected) Color(0xFF4CAF50) else Color(0xFFEF5350)
                                )
                        )
                    }
                }
                Text(
                    text = when {
                        isConnecting -> "..."
                        isConnected -> "MENU"
                        else -> "MENU"
                    },
                    style = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = terminalSkin.foreground
                    )
                )
            }
        }
    }
}
