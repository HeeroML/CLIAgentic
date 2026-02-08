package com.cliagentic.mobileterminal

import com.cliagentic.mobileterminal.data.model.WatchRule
import com.cliagentic.mobileterminal.data.model.WatchRuleType
import com.cliagentic.mobileterminal.terminal.WatchRuleMatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WatchRuleMatcherTest {

    private val matcher = WatchRuleMatcher()

    @Test
    fun `prefix and regex rules both match`() {
        val rules = listOf(
            WatchRule(pattern = "BUILD SUCCESS", type = WatchRuleType.PREFIX),
            WatchRule(pattern = "error:.*", type = WatchRuleType.REGEX)
        )

        val lines = listOf(
            "BUILD SUCCESS in 23s",
            "error: failed to compile module"
        )

        val matches = matcher.match(lines, rules)

        assertThat(matches).hasSize(2)
        assertThat(matches.map { it.rulePattern }).containsExactly("BUILD SUCCESS", "error:.*")
    }

    @Test
    fun `invalid regex does not crash and yields no match`() {
        val rules = listOf(
            WatchRule(pattern = "[unclosed", type = WatchRuleType.REGEX)
        )

        val matches = matcher.match(listOf("something"), rules)

        assertThat(matches).isEmpty()
    }
}
