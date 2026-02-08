package com.cliagentic.mobileterminal.ui.screens.session

import android.view.KeyEvent as AndroidKeyEvent

internal data class ControlTextDelta(
    val backspaces: Int,
    val appended: String
)

internal fun forwardControlKeyEvent(
    nativeEvent: AndroidKeyEvent,
    onSendBytes: (ByteArray) -> Unit,
    onSendLiteral: (String) -> Unit
): Boolean {
    if (nativeEvent.action != AndroidKeyEvent.ACTION_DOWN) return false

    if (nativeEvent.isCtrlPressed) {
        val ctrlByte = ctrlByteForKeyCode(nativeEvent.keyCode)
        if (ctrlByte != null) {
            onSendBytes(byteArrayOf(ctrlByte))
            return true
        }
    }

    when (nativeEvent.keyCode) {
        AndroidKeyEvent.KEYCODE_ENTER -> {
            onSendBytes(byteArrayOf('\r'.code.toByte()))
            return true
        }
        AndroidKeyEvent.KEYCODE_DEL -> {
            onSendBytes(byteArrayOf(0x7F))
            return true
        }
        AndroidKeyEvent.KEYCODE_TAB -> {
            onSendBytes(byteArrayOf('\t'.code.toByte()))
            return true
        }
        AndroidKeyEvent.KEYCODE_ESCAPE -> {
            onSendBytes(byteArrayOf(0x1B))
            return true
        }
        AndroidKeyEvent.KEYCODE_DPAD_UP -> {
            onSendLiteral("\u001B[A")
            return true
        }
        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
            onSendLiteral("\u001B[B")
            return true
        }
        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
            onSendLiteral("\u001B[C")
            return true
        }
        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
            onSendLiteral("\u001B[D")
            return true
        }
    }
    return false
}

internal fun applyControlTextDelta(
    oldValue: String,
    newValue: String,
    ctrlArmed: Boolean,
    onSendBytes: (ByteArray) -> Unit,
    onSendLiteral: (String) -> Unit
) {
    val delta = computeControlTextDelta(oldValue, newValue)
    repeat(delta.backspaces) {
        onSendBytes(byteArrayOf(0x7F))
    }
    if (delta.appended.isNotEmpty()) {
        if (ctrlArmed) {
            val control = ctrlByteForChar(delta.appended.first())
            if (control != null) {
                onSendBytes(byteArrayOf(control))
                val remainder = delta.appended.drop(1)
                if (remainder.isNotEmpty()) onSendLiteral(remainder)
            } else {
                onSendLiteral(delta.appended)
            }
        } else {
            onSendLiteral(delta.appended)
        }
    }
}

internal fun computeControlTextDelta(oldValue: String, newValue: String): ControlTextDelta {
    var prefix = 0
    val maxPrefix = minOf(oldValue.length, newValue.length)
    while (prefix < maxPrefix && oldValue[prefix] == newValue[prefix]) prefix += 1

    var oldSuffix = oldValue.length
    var newSuffix = newValue.length
    while (oldSuffix > prefix && newSuffix > prefix && oldValue[oldSuffix - 1] == newValue[newSuffix - 1]) {
        oldSuffix -= 1
        newSuffix -= 1
    }

    return ControlTextDelta(
        backspaces = oldSuffix - prefix,
        appended = newValue.substring(prefix, newSuffix)
    )
}

internal fun ctrlByteForKeyCode(keyCode: Int): Byte? {
    return when {
        keyCode in AndroidKeyEvent.KEYCODE_A..AndroidKeyEvent.KEYCODE_Z ->
            (keyCode - AndroidKeyEvent.KEYCODE_A + 1).toByte()
        keyCode == AndroidKeyEvent.KEYCODE_SPACE -> 0x00
        keyCode == AndroidKeyEvent.KEYCODE_LEFT_BRACKET -> 0x1B
        keyCode == AndroidKeyEvent.KEYCODE_BACKSLASH -> 0x1C
        keyCode == AndroidKeyEvent.KEYCODE_RIGHT_BRACKET -> 0x1D
        keyCode == AndroidKeyEvent.KEYCODE_6 -> 0x1E
        keyCode == AndroidKeyEvent.KEYCODE_MINUS -> 0x1F
        else -> null
    }
}

internal fun ctrlByteForChar(char: Char): Byte? {
    return when (char.uppercaseChar()) {
        in 'A'..'Z' -> (char.uppercaseChar().code - 'A'.code + 1).toByte()
        ' ' -> 0x00
        '[' -> 0x1B
        '\\' -> 0x1C
        ']' -> 0x1D
        '^' -> 0x1E
        '_' -> 0x1F
        '?' -> 0x7F.toByte()
        else -> null
    }
}
