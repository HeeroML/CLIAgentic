package com.cliagentic.mobileterminal.ui.screens.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cliagentic.mobileterminal.data.model.TerminalSkin
import com.cliagentic.mobileterminal.ui.theme.JetBrainsMono

@Composable
internal fun ConnectionStatusBanner(
    isConnecting: Boolean,
    isPreparingTmux: Boolean,
    commandPromptId: Int,
    commandRunning: Boolean,
    lastExitCode: Int?,
    forceHidden: Boolean = false,
    terminalSkin: TerminalSkin,
    modifier: Modifier = Modifier
) {
    val statusText = when {
        isConnecting -> "Connecting..."
        isPreparingTmux -> "Preparing tmux..."
        commandRunning && commandPromptId >= 0 -> "Prompt #$commandPromptId running..."
        commandRunning -> "Command running..."
        lastExitCode != null && commandPromptId >= 0 -> "Prompt #$commandPromptId exit=$lastExitCode"
        lastExitCode != null -> "Last exit=$lastExitCode"
        commandPromptId >= 0 -> "Prompt #$commandPromptId ready"
        else -> ""
    }
    val showSpinner = isConnecting || isPreparingTmux

    AnimatedVisibility(
        visible = !forceHidden && statusText.isNotBlank(),
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = terminalSkin.selection.copy(alpha = 0.85f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSpinner) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp,
                        color = terminalSkin.accent
                    )
                }
                Text(
                    text = statusText,
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
