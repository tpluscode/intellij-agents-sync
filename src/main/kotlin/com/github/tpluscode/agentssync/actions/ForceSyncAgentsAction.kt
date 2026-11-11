package com.github.tpluscode.agentssync.actions

import com.github.tpluscode.agentssync.service.AgentFileManager
import com.github.tpluscode.agentssync.service.AgentSyncSettingsService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

/**
 * Action to force a manual synchronization of AGENTS.md between memory, local file, and cloud settings.
 */
class ForceSyncAgentsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "Sync Global AGENTS.md"
        e.presentation.description = "Manually synchronize system AGENTS.md with disk and cloud settings"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val fileManager = AgentFileManager.getInstance()
        val settings = AgentSyncSettingsService.getInstance()

        // 1. Write state to disk file
        val targetPath = AgentFileManager.resolvePath(settings.state.customFilePath)
        fileManager.writeFile(targetPath, settings.getContent())

        // 2. Synchronize agent symlinks
        if (settings.state.symlinksEnabled) {
            fileManager.updateSymlinks()
        }

        // 3. Notify settings updated
        settings.notifySettingsUpdated()

        Messages.showInfoMessage(
            e.project,
            "AGENTS.md has been synchronized with $targetPath and agent symlinks have been updated.",
            "Sync Complete"
        )
    }
}
