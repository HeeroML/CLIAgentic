package com.cliagentic.mobileterminal.ssh.v2

import com.cliagentic.mobileterminal.ssh.CommandBoundaryType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Osc133ParserTest {

    @Test
    fun `parses prompt and command lifecycle markers`() {
        val parser = Osc133Parser()

        val prompt = parser.consume("\u001B]133;A\u0007".toByteArray(Charsets.ISO_8859_1))
        assertThat(prompt).hasSize(1)
        assertThat(prompt.first().promptId).isEqualTo(1)
        assertThat(prompt.first().type).isEqualTo(CommandBoundaryType.PROMPT)

        val lifecycle = parser.consume(
            "\u001B]133;B\u0007\u001B]133;C\u0007\u001B]133;D;42\u0007"
                .toByteArray(Charsets.ISO_8859_1)
        )
        assertThat(lifecycle.map { it.type }).containsExactly(
            CommandBoundaryType.COMMAND_INPUT_START,
            CommandBoundaryType.COMMAND_OUTPUT_START,
            CommandBoundaryType.COMMAND_FINISHED
        ).inOrder()
        assertThat(lifecycle.last().promptId).isEqualTo(1)
        assertThat(lifecycle.last().exitCode).isEqualTo(42)
    }

    @Test
    fun `handles marker split across chunks`() {
        val parser = Osc133Parser()

        val first = parser.consume("\u001B]133".toByteArray(Charsets.ISO_8859_1))
        val second = parser.consume(";A\u001B\\".toByteArray(Charsets.ISO_8859_1))

        assertThat(first).isEmpty()
        assertThat(second).hasSize(1)
        assertThat(second.first().type).isEqualTo(CommandBoundaryType.PROMPT)
        assertThat(second.first().promptId).isEqualTo(1)
    }
}
