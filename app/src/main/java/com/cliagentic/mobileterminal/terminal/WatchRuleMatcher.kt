package com.cliagentic.mobileterminal.terminal

import com.cliagentic.mobileterminal.data.model.WatchMatch
import com.cliagentic.mobileterminal.data.model.WatchRule
import com.cliagentic.mobileterminal.data.model.WatchRuleType

class WatchRuleMatcher {
    fun match(lines: List<String>, rules: List<WatchRule>): List<WatchMatch> {
        if (lines.isEmpty() || rules.isEmpty()) return emptyList()

        val matches = mutableListOf<WatchMatch>()

        lines.forEach { line ->
            rules.forEach { rule ->
                val matched = when (rule.type) {
                    WatchRuleType.PREFIX -> {
                        if (rule.caseSensitive) {
                            line.startsWith(rule.pattern)
                        } else {
                            line.startsWith(rule.pattern, ignoreCase = true)
                        }
                    }

                    WatchRuleType.REGEX -> {
                        runCatching {
                            val option = if (rule.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                            Regex(rule.pattern, option).containsMatchIn(line)
                        }.getOrDefault(false)
                    }
                }

                if (matched) {
                    matches += WatchMatch(rulePattern = rule.pattern, snippet = line.take(240))
                }
            }
        }

        return matches
    }
}
