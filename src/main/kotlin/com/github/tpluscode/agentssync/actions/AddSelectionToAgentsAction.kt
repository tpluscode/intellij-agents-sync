package com.github.tpluscode.agentssync.actions

import com.github.tpluscode.agentssync.service.AgentSyncSettingsService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages

/**
 * Action to append selected editor text or snippet as a rule to the system-wide AGENTS.md.
 */
class AddSelectionToAgentsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val selectedText = editor?.selectionModel?.selectedText
        val hasSelection = !selectedText.isNullOrBlank()

        e.presentation.isEnabledAndVisible = hasSelection
        e.presentation.text = "Add Selection to Global AGENTS.md"
        e.presentation.description = "Append the selected code snippet or guideline to the synchronized AGENTS.md"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return

        val comment = Messages.showInputDialog(
            e.project,
            "Enter an optional description / summary for this guideline rule:",
            "Add to Global AGENTS.md",
            Messages.getQuestionIcon(),
            "Guideline from code editor",
            null
        ) ?: return

        val ruleToAdd = if (selectedText.startsWith("```") || selectedText.startsWith("#") || selectedText.startsWith("-")) {
            selectedText
        } else {
            "- $selectedText"
        }

        val service = AgentSyncSettingsService.getInstance()
        service.appendRule(ruleToAdd, "User Action", comment)
        Messages.showInfoMessage(e.project, "Rule successfully appended to global AGENTS.md and synced.", "Rule Added")
    }
}
