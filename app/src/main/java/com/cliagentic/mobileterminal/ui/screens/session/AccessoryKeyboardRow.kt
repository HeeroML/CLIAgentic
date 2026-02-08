package com.cliagentic.mobileterminal.ui.screens.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cliagentic.mobileterminal.data.model.TerminalInputMode
import com.cliagentic.mobileterminal.data.model.TerminalSkin
import com.cliagentic.mobileterminal.ui.theme.JetBrainsMono

@Composable
internal fun AccessoryKeyboardRow(
    visible: Boolean,
    enabled: Boolean,
    inputMode: TerminalInputMode,
    ctrlArmed: Boolean,
    terminalSkin: TerminalSkin,
    onSendBytes: (ByteArray) -> Unit,
    onToggleCtrl: () -> Unit,
    onInputModeChange: (TerminalInputMode) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                AccessoryKey(
                    label = "RAW",
                    terminalSkin = terminalSkin,
                    enabled = enabled,
                    selected = inputMode == TerminalInputMode.CONTROL
                ) {
                    val next = if (inputMode == TerminalInputMode.CONTROL) {
                        TerminalInputMode.PROMPT
                    } else {
                        TerminalInputMode.CONTROL
                    }
                    onInputModeChange(next)
                }
            }
            item {
                AccessoryKey("Tab", terminalSkin, enabled) {
                    onSendBytes(byteArrayOf('\t'.code.toByte()))
                }
            }
            item {
                AccessoryKey(
                    label = "Ctrl",
                    terminalSkin = terminalSkin,
                    enabled = enabled,
                    selected = ctrlArmed
                ) {
                    onToggleCtrl()
                }
            }
            item {
                AccessoryKey("Esc", terminalSkin, enabled) {
                    onSendBytes(byteArrayOf(0x1B))
                }
            }
            item {
                AccessoryKey("|", terminalSkin, enabled) {
                    onSendBytes("|".toByteArray())
                }
            }
            item {
                AccessoryKey("~", terminalSkin, enabled) {
                    onSendBytes("~".toByteArray())
                }
            }
            item {
                AccessoryKey("/", terminalSkin, enabled) {
                    onSendBytes("/".toByteArray())
                }
            }
            item {
                AccessoryKey("-", terminalSkin, enabled) {
                    onSendBytes("-".toByteArray())
                }
            }
            item {
                AccessoryKey("\u2190", terminalSkin, enabled) {
                    onSendBytes("\u001B[D".toByteArray())
                }
            }
            item {
                AccessoryKey("\u2193", terminalSkin, enabled) {
                    onSendBytes("\u001B[B".toByteArray())
                }
            }
            item {
                AccessoryKey("\u2191", terminalSkin, enabled) {
                    onSendBytes("\u001B[A".toByteArray())
                }
            }
            item {
                AccessoryKey("\u2192", terminalSkin, enabled) {
                    onSendBytes("\u001B[C".toByteArray())
                }
            }
        }
    }
}

@Composable
private fun AccessoryKey(
    label: String,
    terminalSkin: TerminalSkin,
    enabled: Boolean,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 32.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) terminalSkin.accent.copy(alpha = 0.8f)
        else terminalSkin.selection.copy(alpha = 0.85f),
        contentColor = if (selected) terminalSkin.background else terminalSkin.foreground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) terminalSkin.background else terminalSkin.foreground
                )
            )
        }
    }
}
