package com.cliagentic.mobileterminal

import com.cliagentic.mobileterminal.terminal.TmuxSessionDiscovery
import com.cliagentic.mobileterminal.terminal.TmuxSessionDiscoveryParser
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TmuxSessionDiscoveryParserTest {

    @Test
    fun `returns missing when marker is present`() {
        val result = TmuxSessionDiscoveryParser.parse(
            stdout = "${TmuxSessionDiscoveryParser.MISSING_MARKER}\n",
            exitCode = 127
        )

        assertThat(result).isEqualTo(TmuxSessionDiscovery.Missing)
    }

    @Test
    fun `returns distinct sessions when tmux exists`() {
        val result = TmuxSessionDiscoveryParser.parse(
            stdout = "dev\nwork\ndev\n",
            exitCode = 0
        )

        assertThat(result).isInstanceOf(TmuxSessionDiscovery.Found::class.java)
        val sessions = (result as TmuxSessionDiscovery.Found).sessions
        assertThat(sessions).containsExactly("dev", "work")
    }
}
