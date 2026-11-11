package com.github.tpluscode.agentssync.service

import com.github.tpluscode.agentssync.model.AgentHistorySnapshot
import com.github.tpluscode.agentssync.model.AgentSymlinkConfig
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service responsible for bidirectional disk synchronization of the AGENTS.md file
 * and maintaining symlinks for all supported AI agents in their default locations.
 */
@Service(Service.Level.APP)
class AgentFileManager {

    private val LOG = Logger.getInstance(AgentFileManager::class.java)
    private val isWriting = AtomicBoolean(false)

    companion object {
        fun getInstance(): AgentFileManager {
            return ApplicationManager.getApplication().getService(AgentFileManager::class.java)
        }

        fun resolvePath(rawPath: String): Path {
            val userHome = System.getProperty("user.home") ?: ""
            val resolved = if (rawPath.startsWith("~")) {
                rawPath.replaceFirst("~", userHome)
            } else {
                rawPath
            }
            return Paths.get(resolved).toAbsolutePath().normalize()
        }
    }

    /**
     * Initializes file mirroring and symlinks on startup.
     */
    fun initialize() {
        val settings = AgentSyncSettingsService.getInstance()
        val state = settings.state
        val filePath = resolvePath(state.customFilePath)

        try {
            if (Files.exists(filePath)) {
                // If local file exists, check if we should pull from it or push to it
                val localContent = Files.readString(filePath, Charsets.UTF_8)
                val localHash = AgentHistorySnapshot.computeHash(localContent)
                val stateHash = AgentHistorySnapshot.computeHash(settings.getContent())

                if (localHash != stateHash) {
                    if (state.autoSyncFromFile && localContent.isNotBlank()) {
                        LOG.info("Syncing local AGENTS.md from $filePath into Settings Sync")
                        settings.updateContent(localContent, "Local File", "Loaded from local file on startup")
                    } else if (state.autoSyncToFile) {
                        writeFile(filePath, settings.getContent())
                    }
                }
            } else {
                // File doesn't exist yet, create parent dirs and write current content
                if (state.autoSyncToFile) {
                    writeFile(filePath, settings.getContent())
                }
            }

            if (state.symlinksEnabled) {
                updateSymlinks()
            }
        } catch (e: Exception) {
            LOG.warn("Error initializing AGENTS.md at $filePath", e)
        }

        // Subscribe to state change events to mirror writes
        val busConnection = ApplicationManager.getApplication().messageBus.connect()
        busConnection.subscribe(AgentSyncListener.TOPIC, object : AgentSyncListener {
            override fun onContentUpdated(newContent: String, author: String, message: String) {
                if (isWriting.get()) return
                val currentSettings = AgentSyncSettingsService.getInstance().state
                if (currentSettings.autoSyncToFile) {
                    val targetPath = resolvePath(currentSettings.customFilePath)
                    writeFile(targetPath, newContent)
                }
                if (currentSettings.symlinksEnabled) {
                    updateSymlinks()
                }
            }

            override fun onSettingsUpdated() {
                val currentSettings = AgentSyncSettingsService.getInstance().state
                if (currentSettings.autoSyncToFile) {
                    val targetPath = resolvePath(currentSettings.customFilePath)
                    writeFile(targetPath, AgentSyncSettingsService.getInstance().getContent())
                }
                if (currentSettings.symlinksEnabled) {
                    updateSymlinks()
                }
            }
        })
    }

    /**
     * Writes content to the local disk file safely.
     */
    fun writeFile(path: Path, content: String) {
        try {
            isWriting.set(true)
            val parent = path.parent
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent)
            }

            val currentFileContent = if (Files.exists(path)) Files.readString(path, Charsets.UTF_8) else null
            if (currentFileContent != content) {
                Files.writeString(path, content, Charsets.UTF_8)
                LOG.info("Updated mirrored AGENTS.md at $path")
            }
        } catch (e: Exception) {
            LOG.error("Failed to write AGENTS.md to $path", e)
        } finally {
            isWriting.set(false)
        }
    }

    /**
     * Updates symlinks for all enabled agent configurations pointing to designated AGENTS.md file.
     */
    fun updateSymlinks() {
        val settings = AgentSyncSettingsService.getInstance()
        val state = settings.state
        val designatedPath = resolvePath(state.customFilePath)

        // Make sure designated file exists
        if (!Files.exists(designatedPath)) {
            writeFile(designatedPath, settings.getContent())
        }

        for (config in state.agentSymlinks) {
            if (!config.enabled) continue
            val targetPath = resolvePath(config.targetPath)
            createOrUpdateSymlink(targetPath, designatedPath)
        }
    }

    /**
     * Creates or updates a symbolic link from targetPath to designatedPath.
     */
    fun createOrUpdateSymlink(targetPath: Path, designatedPath: Path): Boolean {
        val normalizedTarget = targetPath.toAbsolutePath().normalize()
        val normalizedDesignated = designatedPath.toAbsolutePath().normalize()

        if (normalizedTarget == normalizedDesignated) {
            return true // Source designated file itself
        }

        try {
            val parent = normalizedTarget.parent
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent)
            }

            if (Files.isSymbolicLink(normalizedTarget)) {
                try {
                    val currentTarget = Files.readSymbolicLink(normalizedTarget)
                    val resolvedTarget = normalizedTarget.parent?.resolve(currentTarget)?.normalize() ?: currentTarget.normalize()
                    if (resolvedTarget == normalizedDesignated) {
                        return true
                    }
                } catch (_: Exception) {}
                Files.deleteIfExists(normalizedTarget)
            } else if (Files.exists(normalizedTarget)) {
                // If it's a regular file, create a backup if non-empty and different
                try {
                    val existingContent = Files.readString(normalizedTarget, Charsets.UTF_8)
                    val designatedContent = if (Files.exists(normalizedDesignated)) Files.readString(normalizedDesignated, Charsets.UTF_8) else ""
                    if (existingContent.isNotBlank() && existingContent != designatedContent) {
                        val backupPath = Paths.get("${normalizedTarget}.backup")
                        Files.copy(normalizedTarget, backupPath, StandardCopyOption.REPLACE_EXISTING)
                        LOG.info("Created backup for existing file at $backupPath")
                    }
                } catch (e: Exception) {
                    LOG.warn("Could not check/backup existing file before symlinking at $normalizedTarget", e)
                }
                Files.deleteIfExists(normalizedTarget)
            }

            Files.createSymbolicLink(normalizedTarget, normalizedDesignated)
            LOG.info("Created symlink: $normalizedTarget -> $normalizedDesignated")
            return true
        } catch (e: Exception) {
            LOG.warn("Failed to create symlink at $normalizedTarget -> $normalizedDesignated. Falling back to copy.", e)
            try {
                if (Files.exists(normalizedDesignated)) {
                    Files.copy(normalizedDesignated, normalizedTarget, StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (copyEx: Exception) {
                LOG.error("Failed fallback copy to $normalizedTarget", copyEx)
            }
            return false
        }
    }

    /**
     * Determines the current link status of an agent target path.
     */
    fun getSymlinkStatus(targetPath: Path, designatedPath: Path): String {
        val normalizedTarget = targetPath.toAbsolutePath().normalize()
        val normalizedDesignated = designatedPath.toAbsolutePath().normalize()

        if (normalizedTarget == normalizedDesignated) {
            return "Designated Shared File"
        }

        if (Files.isSymbolicLink(normalizedTarget)) {
            return try {
                val currentTarget = Files.readSymbolicLink(normalizedTarget)
                val resolvedTarget = normalizedTarget.parent?.resolve(currentTarget)?.normalize() ?: currentTarget.normalize()
                if (resolvedTarget == normalizedDesignated) {
                    "Linked"
                } else {
                    "Points to $currentTarget"
                }
            } catch (e: Exception) {
                "Broken Symlink"
            }
        }

        if (Files.exists(normalizedTarget)) {
            return "Regular File"
        }

        return "Not Created"
    }

    /**
     * Reloads content from the local disk file into state if changed.
     */
    fun reloadFromDisk(): Boolean {
        val settings = AgentSyncSettingsService.getInstance()
        val path = resolvePath(settings.state.customFilePath)
        if (!Files.exists(path)) return false

        return try {
            val content = Files.readString(path, Charsets.UTF_8)
            val localHash = AgentHistorySnapshot.computeHash(content)
            val stateHash = AgentHistorySnapshot.computeHash(settings.getContent())

            if (localHash != stateHash) {
                settings.updateContent(content, "Disk Reload", "Reloaded from disk at $path")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            LOG.warn("Failed to reload AGENTS.md from $path", e)
            false
        }
    }

    /**
     * Gets the resolved java.io.File instance.
     */
    fun getLocalFile(): File {
        val settings = AgentSyncSettingsService.getInstance()
        return resolvePath(settings.state.customFilePath).toFile()
    }
}
