package com.cliagentic.mobileterminal.data.model

enum class TmuxPrefix(val label: String, val controlByte: Byte) {
    CTRL_A("Ctrl+A", 0x01),
    CTRL_B("Ctrl+B", 0x02),
    CTRL_SPACE("Ctrl+Space", 0x00);

    companion object {
        fun fromNameOrDefault(value: String?): TmuxPrefix {
            return entries.firstOrNull { it.name == value } ?: CTRL_B
        }
    }
}
