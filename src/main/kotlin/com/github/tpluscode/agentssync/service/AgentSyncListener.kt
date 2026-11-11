package com.github.tpluscode.agentssync.service

import com.intellij.util.messages.Topic

/**
 * Listener for changes to the system-wide AGENTS.md content or settings.
 */
interface AgentSyncListener {
    companion object {
        val TOPIC = Topic.create("Agents Sync Topic", AgentSyncListener::class.java)
    }

    /**
     * Called when the AGENTS.md content is updated.
     */
    fun onContentUpdated(newContent: String, author: String, message: String) {}

    /**
     * Called when settings (paths, ports, sync options) are updated.
     */
    fun onSettingsUpdated() {}
}
