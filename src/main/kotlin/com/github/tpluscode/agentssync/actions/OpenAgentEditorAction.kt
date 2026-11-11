package com.github.tpluscode.agentssync.actions

import com.github.tpluscode.agentssync.service.AgentFileManager
import com.github.tpluscode.agentssync.service.AgentSyncSettingsService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.nio.file.Files

/**
 * Action to open the system-wide AGENTS.md directly in a full IntelliJ editor tab.
 */
class OpenAgentEditorAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        e.presentation.text = "Open Global AGENTS.md Editor"
        e.presentation.description = "Open the synchronized system AGENTS.md in a full Markdown editor tab"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val fileManager = AgentFileManager.getInstance()
        val settings = AgentSyncSettingsService.getInstance()
        val file = fileManager.getLocalFile()

        if (!file.exists()) {
            fileManager.writeFile(file.toPath(), settings.getContent())
        }

        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }
}
