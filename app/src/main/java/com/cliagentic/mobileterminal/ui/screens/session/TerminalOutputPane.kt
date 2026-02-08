package com.cliagentic.mobileterminal.ui.screens.session

import android.graphics.Typeface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cliagentic.mobileterminal.data.model.TerminalSkin
import com.cliagentic.mobileterminal.ui.theme.JetBrainsMono
import org.connectbot.terminal.Terminal
import org.connectbot.terminal.TerminalEmulator

@Composable
internal fun TerminalOutputPane(
    terminalEmulator: TerminalEmulator?,
    terminalText: String,
    isConnected: Boolean,
    terminalSkin: TerminalSkin,
    keyboardEnabled: Boolean,
    showSoftKeyboard: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    if (terminalEmulator != null) {
        Terminal(
            terminalEmulator = terminalEmulator,
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            typeface = Typeface.MONOSPACE,
            backgroundColor = terminalSkin.background,
            foregroundColor = terminalSkin.foreground,
            keyboardEnabled = keyboardEnabled,
            showSoftKeyboard = showSoftKeyboard,
            focusRequester = focusRequester
        )
        return
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(terminalText.length) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    SelectionContainer(modifier = modifier) {
        Text(
            text = if (terminalText.isBlank()) {
                if (isConnected) "Waiting for output..." else "Not connected"
            } else {
                terminalText
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(scrollState),
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (terminalText.isBlank()) terminalSkin.dimText else terminalSkin.foreground
            )
        )
    }
}
