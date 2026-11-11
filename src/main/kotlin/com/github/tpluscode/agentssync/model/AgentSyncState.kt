package com.github.tpluscode.agentssync.model

import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.XCollection

/**
 * Persistent state for Agents Sync settings and global AGENTS.md document.
 */
data class AgentSyncState(
    var content: String = DEFAULT_CONTENT,
    var customFilePath: String = DEFAULT_FILE_PATH,
    var autoSyncToFile: Boolean = true,
    var autoSyncFromFile: Boolean = true,
    var symlinksEnabled: Boolean = true,
    var mcpEnabled: Boolean = true,
    var mcpPort: Int = DEFAULT_MCP_PORT,
    var historyLimit: Int = 100,

    @get:XCollection(style = XCollection.Style.v2)
    var agentSymlinks: MutableList<AgentSymlinkConfig> = AgentSymlinkConfig.createDefaults(),

    @get:XCollection(style = XCollection.Style.v2)
    var history: MutableList<AgentHistorySnapshot> = mutableListOf()
) {
    companion object {
        const val DEFAULT_FILE_PATH = "~/.junie/AGENTS.md"
        const val DEFAULT_MCP_PORT = 8765

        val DEFAULT_CONTENT = """
            # Global AGENTS.md Guidelines

            This file defines universal conventions, persona instructions, and workflow rules shared across all projects and machines.

            ## Core Principles
            - Write clean, idiomatic, and maintainable code.
            - Follow repository code style, formatting, and naming conventions.
            - Include focused automated tests for non-trivial logic.
            - Provide clear, concise commit messages and summary notes.

            ## AI Agent Instructions
            - Before modifying code, analyze existing patterns in the project.
            - Prefer minimal, safe diffs over unnecessary rewrites.
            - Avoid introducing unused dependencies.
        """.trimIndent()
    }
}
