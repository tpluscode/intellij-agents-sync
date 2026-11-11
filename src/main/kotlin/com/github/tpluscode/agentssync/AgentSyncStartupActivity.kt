package com.github.tpluscode.agentssync

import com.github.tpluscode.agentssync.mcp.AgentMcpServerService
import com.github.tpluscode.agentssync.service.AgentFileManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Startup activity that initializes file mirroring and the MCP server when an IDE project opens.
 */
class AgentSyncStartupActivity : ProjectActivity {

    companion object {
        private val initialized = AtomicBoolean(false)
    }

    override suspend fun execute(project: Project) {
        if (initialized.compareAndSet(false, true)) {
            AgentFileManager.getInstance().initialize()
            AgentMcpServerService.getInstance().start()
        }
    }
}
