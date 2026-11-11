package com.github.tpluscode.agentssync.settings

import com.github.tpluscode.agentssync.service.AgentSyncSettingsService
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

/**
 * Configurable entry for Agents Sync under Settings -> Tools -> Agents Sync.
 */
class AgentSyncConfigurable : SearchableConfigurable {

    private var settingsPanel: AgentSyncSettingsPanel? = null

    override fun getId(): String = "com.github.tpluscode.agentssync.settings.AgentSyncConfigurable"

    override fun getDisplayName(): String = "Agents Sync (AGENTS.md)"

    override fun createComponent(): JComponent {
        val panel = AgentSyncSettingsPanel()
        settingsPanel = panel
        return panel.mainPanel
    }

    override fun isModified(): Boolean {
        val panel = settingsPanel ?: return false
        val state = AgentSyncSettingsService.getInstance().state
        return panel.isModified(state)
    }

    override fun apply() {
        val panel = settingsPanel ?: return
        val settings = AgentSyncSettingsService.getInstance()
        panel.apply(settings.state)
        settings.notifySettingsUpdated()
    }

    override fun reset() {
        val panel = settingsPanel ?: return
        val state = AgentSyncSettingsService.getInstance().state
        panel.reset(state)
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
