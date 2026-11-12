package com.github.tpluscode.agentssync

import com.github.tpluscode.agentssync.model.AgentHistorySnapshot
import com.github.tpluscode.agentssync.model.AgentSymlinkConfig
import com.github.tpluscode.agentssync.model.AgentSyncState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgentSyncStateTest {

    @Test
    fun `test default state values`() {
        val state = AgentSyncState()
        assertTrue(state.content.contains("# Global AGENTS.md Guidelines"))
        assertEquals("~/.junie/AGENTS.md", state.customFilePath)
        assertTrue(state.autoSyncToFile)
        assertTrue(state.autoSyncFromFile)
        assertTrue(state.symlinksEnabled)
        assertTrue(state.mcpEnabled)
        assertEquals(8765, state.mcpPort)
        assertEquals(100, state.historyLimit)
        assertTrue(state.history.isEmpty())
        assertTrue(state.agentSymlinks.isNotEmpty())
    }

    @Test
    fun `test default agent symlinks contain standard agents`() {
        val defaults = AgentSymlinkConfig.createDefaults()
        val ids = defaults.map { it.id }
        assertTrue(ids.contains("junie"))
        assertTrue(ids.contains("codex"))
        assertTrue(ids.contains("claude"))
        assertTrue(ids.contains("cursor"))
        assertTrue(ids.contains("windsurf"))
        assertTrue(ids.contains("aider"))
        assertTrue(ids.contains("copilot"))
        assertTrue(ids.contains("cline"))
        assertTrue(ids.contains("roo"))
        assertTrue(ids.contains("user_home"))
        assertTrue(ids.contains("xdg_config"))
    }

    @Test
    fun `test snapshot creation and hash computation`() {
        val content = "# Test Guidelines\n- Rule 1\n- Rule 2"
        val snapshot = AgentHistorySnapshot.create(content, "AI Assistant", "Added rule 2")

        assertNotNull(snapshot.id)
        assertTrue(snapshot.timestamp > 0)
        assertEquals("AI Assistant", snapshot.author)
        assertEquals("Added rule 2", snapshot.message)
        assertEquals(content, snapshot.content)

        val expectedHash = AgentHistorySnapshot.computeHash(content)
        assertEquals(expectedHash, snapshot.contentHash)
        assertFalse(snapshot.contentHash.isBlank())
    }

    @Test
    fun `test hash consistency and differentiation`() {
        val hash1 = AgentHistorySnapshot.computeHash("hello world")
        val hash2 = AgentHistorySnapshot.computeHash("hello world")
        val hash3 = AgentHistorySnapshot.computeHash("hello world!")

        assertEquals(hash1, hash2)
        assertFalse(hash1 == hash3)
    }
}
