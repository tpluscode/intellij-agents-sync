package com.github.tpluscode.agentssync.editor

import com.github.tpluscode.agentssync.service.AgentFileManager
import com.github.tpluscode.agentssync.service.AgentSyncSettingsService
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.vfs.VirtualFile

/**
 * Listens to document save events in IntelliJ editor tabs to synchronize changes
 * back into the AgentSyncSettingsService and cloud Settings Sync.
 */
class AgentFileDocumentListener : FileDocumentManagerListener {

    override fun beforeDocumentSaving(document: Document) {
        val fileDocumentManager = FileDocumentManager.getInstance()
        val virtualFile: VirtualFile = fileDocumentManager.getFile(document) ?: return
        val localTargetFile = AgentFileManager.getInstance().getLocalFile()

        if (virtualFile.path == localTargetFile.absolutePath || virtualFile.name.equals("AGENTS.md", ignoreCase = true)) {
            val updatedText = document.text
            val settings = AgentSyncSettingsService.getInstance()
            if (settings.getContent() != updatedText) {
                settings.updateContent(updatedText, "Editor Tab", "Edited via IntelliJ Markdown Editor")
            }
        }
    }
}
