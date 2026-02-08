package com.cliagentic.mobileterminal.data.model

import androidx.compose.ui.graphics.Color

data class TerminalSkin(
    val id: String,
    val displayName: String,
    val background: Color,
    val foreground: Color,
    val cursor: Color,
    val selection: Color,
    val accent: Color,
    val dimText: Color
)

enum class TerminalSkinId(val displayName: String) {
    DRACULA("Dracula"),
    SOLARIZED_DARK("Solarized Dark"),
    MONOKAI("Monokai"),
    NORD("Nord"),
    GRUVBOX("Gruvbox Dark"),
    TOKYO_NIGHT("Tokyo Night"),
    ONE_DARK("One Dark"),
    CATPPUCCIN("Catppuccin Mocha"),
    TERMINAL_GREEN("Retro Green");

    val skin: TerminalSkin
        get() = TerminalSkins.forId(this)
}

object TerminalSkins {
    val Dracula = TerminalSkin(
        id = "dracula",
        displayName = "Dracula",
        background = Color(0xFF282A36),
        foreground = Color(0xFFF8F8F2),
        cursor = Color(0xFFFF79C6),
        selection = Color(0xFF44475A),
        accent = Color(0xFFBD93F9),
        dimText = Color(0xFF6272A4)
    )

    val SolarizedDark = TerminalSkin(
        id = "solarized_dark",
        displayName = "Solarized Dark",
        background = Color(0xFF002B36),
        foreground = Color(0xFF839496),
        cursor = Color(0xFF93A1A1),
        selection = Color(0xFF073642),
        accent = Color(0xFF268BD2),
        dimText = Color(0xFF586E75)
    )

    val Monokai = TerminalSkin(
        id = "monokai",
        displayName = "Monokai",
        background = Color(0xFF272822),
        foreground = Color(0xFFF8F8F2),
        cursor = Color(0xFFF92672),
        selection = Color(0xFF49483E),
        accent = Color(0xFFA6E22E),
        dimText = Color(0xFF75715E)
    )

    val Nord = TerminalSkin(
        id = "nord",
        displayName = "Nord",
        background = Color(0xFF2E3440),
        foreground = Color(0xFFD8DEE9),
        cursor = Color(0xFF88C0D0),
        selection = Color(0xFF3B4252),
        accent = Color(0xFF81A1C1),
        dimText = Color(0xFF4C566A)
    )

    val Gruvbox = TerminalSkin(
        id = "gruvbox",
        displayName = "Gruvbox Dark",
        background = Color(0xFF282828),
        foreground = Color(0xFFEBDBB2),
        cursor = Color(0xFFFE8019),
        selection = Color(0xFF3C3836),
        accent = Color(0xFFB8BB26),
        dimText = Color(0xFF928374)
    )

    val TokyoNight = TerminalSkin(
        id = "tokyo_night",
        displayName = "Tokyo Night",
        background = Color(0xFF1A1B26),
        foreground = Color(0xFFA9B1D6),
        cursor = Color(0xFF7AA2F7),
        selection = Color(0xFF283457),
        accent = Color(0xFF7DCFFF),
        dimText = Color(0xFF565F89)
    )

    val OneDark = TerminalSkin(
        id = "one_dark",
        displayName = "One Dark",
        background = Color(0xFF282C34),
        foreground = Color(0xFFABB2BF),
        cursor = Color(0xFF528BFF),
        selection = Color(0xFF3E4451),
        accent = Color(0xFF61AFEF),
        dimText = Color(0xFF5C6370)
    )

    val Catppuccin = TerminalSkin(
        id = "catppuccin",
        displayName = "Catppuccin Mocha",
        background = Color(0xFF1E1E2E),
        foreground = Color(0xFFCDD6F4),
        cursor = Color(0xFFF5E0DC),
        selection = Color(0xFF313244),
        accent = Color(0xFFCBA6F7),
        dimText = Color(0xFF6C7086)
    )

    val TerminalGreen = TerminalSkin(
        id = "terminal_green",
        displayName = "Retro Green",
        background = Color(0xFF0A0E14),
        foreground = Color(0xFF33FF00),
        cursor = Color(0xFF33FF00),
        selection = Color(0xFF1A3A1A),
        accent = Color(0xFF33FF00),
        dimText = Color(0xFF1A7A00)
    )

    val all = listOf(
        Dracula, SolarizedDark, Monokai, Nord, Gruvbox,
        TokyoNight, OneDark, Catppuccin, TerminalGreen
    )

    fun forId(id: TerminalSkinId): TerminalSkin = when (id) {
        TerminalSkinId.DRACULA -> Dracula
        TerminalSkinId.SOLARIZED_DARK -> SolarizedDark
        TerminalSkinId.MONOKAI -> Monokai
        TerminalSkinId.NORD -> Nord
        TerminalSkinId.GRUVBOX -> Gruvbox
        TerminalSkinId.TOKYO_NIGHT -> TokyoNight
        TerminalSkinId.ONE_DARK -> OneDark
        TerminalSkinId.CATPPUCCIN -> Catppuccin
        TerminalSkinId.TERMINAL_GREEN -> TerminalGreen
    }

    fun fromString(id: String): TerminalSkin =
        all.firstOrNull { it.id == id } ?: Dracula
}
