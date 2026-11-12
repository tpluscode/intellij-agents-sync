package com.github.tpluscode.agentssync.model

/**
 * Configuration for an agent-specific symlink targeting the shared AGENTS.md file.
 */
data class AgentSymlinkConfig(
    var id: String = "",
    var name: String = "",
    var targetPath: String = "",
    var enabled: Boolean = true,
    var description: String = ""
) {
    companion object {
        fun createDefaults(): MutableList<AgentSymlinkConfig> {
            return mutableListOf(
                AgentSymlinkConfig(
                    id = "junie",
                    name = "Junie / JetBrains",
                    targetPath = "~/.junie/AGENTS.md",
                    enabled = true,
                    description = "Default instructions for Junie and JetBrains AI agents"
                ),
                AgentSymlinkConfig(
                    id = "codex",
                    name = "OpenAI Codex",
                    targetPath = "~/.codex/AGENTS.md",
                    enabled = true,
                    description = "Global instructions for OpenAI Codex"
                ),
                AgentSymlinkConfig(
                    id = "claude",
                    name = "Claude Code",
                    targetPath = "~/.claude/CLAUDE.md",
                    enabled = true,
                    description = "Global instructions file for Claude CLI and Claude Code"
                ),
                AgentSymlinkConfig(
                    id = "cursor",
                    name = "Cursor",
                    targetPath = "~/.cursorrules",
                    enabled = true,
                    description = "Global rules configuration for Cursor editor"
                ),
                AgentSymlinkConfig(
                    id = "windsurf",
                    name = "Windsurf / Codeium",
                    targetPath = "~/.windsurfrules",
                    enabled = true,
                    description = "Global rules configuration for Windsurf IDE"
                ),
                AgentSymlinkConfig(
                    id = "aider",
                    name = "Aider",
                    targetPath = "~/.aider.conventions.md",
                    enabled = true,
                    description = "Default conventions file for Aider AI coding assistant"
                ),
                AgentSymlinkConfig(
                    id = "copilot",
                    name = "GitHub Copilot",
                    targetPath = "~/.config/github-copilot/instructions.md",
                    enabled = true,
                    description = "Global prompt instructions for GitHub Copilot"
                ),
                AgentSymlinkConfig(
                    id = "cline",
                    name = "Cline",
                    targetPath = "~/.cline/rules/AGENTS.md",
                    enabled = true,
                    description = "Global rules directory for Cline autonomous coding agent"
                ),
                AgentSymlinkConfig(
                    id = "roo",
                    name = "Roo Code",
                    targetPath = "~/.roo/rules/AGENTS.md",
                    enabled = true,
                    description = "Global rules directory for Roo Code"
                ),
                AgentSymlinkConfig(
                    id = "user_home",
                    name = "User Home AGENTS.md",
                    targetPath = "~/.AGENTS.md",
                    enabled = true,
                    description = "Standard user home AGENTS.md for CLI agents and scripts"
                ),
                AgentSymlinkConfig(
                    id = "xdg_config",
                    name = "XDG Config AGENTS.md",
                    targetPath = "~/.config/AGENTS.md",
                    enabled = true,
                    description = "Standard XDG config directory specification"
                )
            )
        }
    }
}
