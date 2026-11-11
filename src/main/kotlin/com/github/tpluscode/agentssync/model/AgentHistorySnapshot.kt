package com.github.tpluscode.agentssync.model

import java.util.UUID

/**
 * Snapshot of AGENTS.md content stored in persistent history.
 */
data class AgentHistorySnapshot(
    var id: String = UUID.randomUUID().toString(),
    var timestamp: Long = System.currentTimeMillis(),
    var author: String = "User",
    var message: String = "Updated AGENTS.md",
    var content: String = "",
    var contentHash: String = ""
) {
    companion object {
        fun create(content: String, author: String = "User", message: String = "Updated AGENTS.md"): AgentHistorySnapshot {
            return AgentHistorySnapshot(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                author = author,
                message = message,
                content = content,
                contentHash = computeHash(content)
            )
        }

        fun computeHash(content: String): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}
