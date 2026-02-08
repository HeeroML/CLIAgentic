package com.cliagentic.mobileterminal.data.model

enum class WatchRuleType {
    PREFIX,
    REGEX
}

data class WatchRule(
    val id: Long = System.nanoTime(),
    val pattern: String,
    val type: WatchRuleType,
    val caseSensitive: Boolean = false
)

data class WatchMatch(
    val rulePattern: String,
    val snippet: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
