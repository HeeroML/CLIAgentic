package com.cliagentic.mobileterminal.ui.screens.session

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cliagentic.mobileterminal.data.model.TerminalSkin
import com.cliagentic.mobileterminal.ui.theme.JetBrainsMono

@Composable
internal fun PromptInputBar(
    visible: Boolean,
    inputDraft: String,
    enabled: Boolean,
    terminalSkin: TerminalSkin,
    focusRequester: FocusRequester,
    onInputDraftChange: (String) -> Unit,
    onSendDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = terminalSkin.selection.copy(alpha = 0.9f),
            contentColor = terminalSkin.foreground
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputDraft,
                    onValueChange = onInputDraftChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (
                                event.nativeKeyEvent.action == AndroidKeyEvent.ACTION_UP &&
                                event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                            ) {
                                onSendDraft()
                                true
                            } else {
                                false
                            }
                        },
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 14.sp,
                        color = terminalSkin.foreground
                    ),
                    cursorBrush = SolidColor(terminalSkin.cursor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendDraft() }),
                    decorationBox = { innerTextField ->
                        if (inputDraft.isEmpty()) {
                            Text(
                                "$ _",
                                style = TextStyle(
                                    fontFamily = JetBrainsMono,
                                    fontSize = 14.sp,
                                    color = terminalSkin.dimText
                                )
                            )
                        }
                        innerTextField()
                    }
                )
                IconButton(
                    onClick = onSendDraft,
                    enabled = enabled,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(18.dp),
                        tint = terminalSkin.accent
                    )
                }
            }
        }
    }
}
