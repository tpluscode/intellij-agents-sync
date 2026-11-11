package com.github.tpluscode.agentssync

import com.github.tpluscode.agentssync.service.AgentFileManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class AgentFileManagerTest {

    @Test
    fun `test resolvePath expands tilde`() {
        val userHome = System.getProperty("user.home")
        val resolved = AgentFileManager.resolvePath("~/.junie/AGENTS.md")
        assertEquals(Path.of(userHome, ".junie", "AGENTS.md").toAbsolutePath().normalize(), resolved)
    }

    @Test
    fun `test resolvePath keeps absolute path unchanged`() {
        val absolutePath = "/tmp/custom/AGENTS.md"
        val resolved = AgentFileManager.resolvePath(absolutePath)
        assertEquals(Path.of(absolutePath).toAbsolutePath().normalize(), resolved)
    }

    @Test
    fun `test writeFile creates directories and contents`(@TempDir tempDir: Path) {
        val fileManager = AgentFileManager()
        val targetPath = tempDir.resolve("sub/nested/AGENTS.md")
        val content = "# Test Guidelines Content"

        fileManager.writeFile(targetPath, content)

        assertTrue(Files.exists(targetPath))
        val readBack = Files.readString(targetPath, Charsets.UTF_8)
        assertEquals(content, readBack)
    }

    @Test
    fun `test createOrUpdateSymlink creates valid symlink and tracks status`(@TempDir tempDir: Path) {
        val fileManager = AgentFileManager()
        val designatedPath = tempDir.resolve("designated/AGENTS.md")
        val content = "# Global AGENTS.md Content"
        fileManager.writeFile(designatedPath, content)

        val claudeTargetPath = tempDir.resolve("claude/CLAUDE.md")
        val cursorTargetPath = tempDir.resolve("cursor/.cursorrules")

        assertEquals("Designated Shared File", fileManager.getSymlinkStatus(designatedPath, designatedPath))
        assertEquals("Not Created", fileManager.getSymlinkStatus(claudeTargetPath, designatedPath))

        // Create symlink
        val created = fileManager.createOrUpdateSymlink(claudeTargetPath, designatedPath)
        assertTrue(created)
        assertTrue(Files.exists(claudeTargetPath))
        assertTrue(Files.isSymbolicLink(claudeTargetPath))
        assertEquals("Linked", fileManager.getSymlinkStatus(claudeTargetPath, designatedPath))
        assertEquals(content, Files.readString(claudeTargetPath, Charsets.UTF_8))

        // Repeat call should be idempotent
        val repeated = fileManager.createOrUpdateSymlink(claudeTargetPath, designatedPath)
        assertTrue(repeated)
        assertEquals("Linked", fileManager.getSymlinkStatus(claudeTargetPath, designatedPath))

        // Test existing regular file backup and conversion to symlink
        fileManager.writeFile(cursorTargetPath, "# Existing Cursor Rules")
        assertEquals("Regular File", fileManager.getSymlinkStatus(cursorTargetPath, designatedPath))

        val converted = fileManager.createOrUpdateSymlink(cursorTargetPath, designatedPath)
        assertTrue(converted)
        assertTrue(Files.isSymbolicLink(cursorTargetPath))
        assertEquals("Linked", fileManager.getSymlinkStatus(cursorTargetPath, designatedPath))
        assertEquals(content, Files.readString(cursorTargetPath, Charsets.UTF_8))

        // Verify backup was created for non-empty existing content
        val backupFile = tempDir.resolve("cursor/.cursorrules.backup")
        assertTrue(Files.exists(backupFile))
        assertEquals("# Existing Cursor Rules", Files.readString(backupFile, Charsets.UTF_8))
    }
}
