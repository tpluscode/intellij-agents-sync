package com.github.tpluscode.agentssync.service

import com.github.tpluscode.agentssync.model.AgentHistorySnapshot
import com.github.tpluscode.agentssync.model.AgentSyncState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Application service that manages the persistent state of AGENTS.md and its synchronization settings.
 * Uses IntelliJ's RoamingType.DEFAULT to sync state across machines via JetBrains Settings Sync.
 */
@Service(Service.Level.APP)
@State(
    name = "com.github.tpluscode.agentssync.AgentSyncSettings",
    storages = [Storage(value = "agents-sync.xml", roamingType = RoamingType.DEFAULT)]
)
class AgentSyncSettingsService : PersistentStateComponent<AgentSyncState> {

    private var myState = AgentSyncState()
    private val lock = ReentrantLock()

    companion object {
        fun getInstance(): AgentSyncSettingsService {
            return ApplicationManager.getApplication().getService(AgentSyncSettingsService::class.java)
        }
    }

    override fun getState(): AgentSyncState {
        return lock.withLock {
            myState
        }
    }

    override fun loadState(loadedState: AgentSyncState) {
        val previousHash: String
        val newHash: String
        val content: String

        lock.withLock {
            previousHash = AgentHistorySnapshot.computeHash(myState.content)
            newHash = AgentHistorySnapshot.computeHash(loadedState.content)
            myState = loadedState
            content = loadedState.content
        }

        if (previousHash != newHash && content.isNotEmpty()) {
            notifyContentUpdated(content, "Settings Sync", "Synchronized from JetBrains Cloud")
        }
    }

    /**
     * Gets the current AGENTS.md markdown content.
     */
    fun getContent(): String {
        return lock.withLock { myState.content }
    }

    /**
     * Updates the AGENTS.md markdown content, automatically creating a history snapshot.
     */
    fun updateContent(newContent: String, author: String = "User", message: String = "Updated AGENTS.md"): Boolean {
        var changed = false
        lock.withLock {
            val currentHash = AgentHistorySnapshot.computeHash(myState.content)
            val newHash = AgentHistorySnapshot.computeHash(newContent)

            if (currentHash != newHash) {
                myState.content = newContent
                recordSnapshotInternal(newContent, author, message)
                changed = true
            }
        }

        if (changed) {
            notifyContentUpdated(newContent, author, message)
        }
        return changed
    }

    /**
     * Appends a rule/section to AGENTS.md content.
     */
    fun appendRule(ruleText: String, author: String = "User", message: String = "Appended guideline rule"): Boolean {
        val current = getContent()
        val updated = if (current.isBlank()) {
            ruleText.trim()
        } else {
            current.trimEnd() + "\n\n" + ruleText.trim() + "\n"
        }
        return updateContent(updated, author, message)
    }

    /**
     * Rolls back AGENTS.md to a historical snapshot.
     */
    fun rollbackToSnapshot(snapshotId: String, author: String = "User"): Boolean {
        var targetContent: String? = null
        var snapshotMsg = ""

        lock.withLock {
            val snapshot = myState.history.find { it.id == snapshotId }
            if (snapshot != null) {
                targetContent = snapshot.content
                snapshotMsg = "Rolled back to snapshot from ${snapshot.message}"
            }
        }

        return targetContent?.let {
            updateContent(it, author, snapshotMsg)
        } ?: false
    }

    /**
     * Returns a copy of the history list.
     */
    fun getHistory(): List<AgentHistorySnapshot> {
        return lock.withLock {
            myState.history.toList()
        }
    }

    /**
     * Clears history snapshots.
     */
    fun clearHistory() {
        lock.withLock {
            myState.history.clear()
        }
        notifySettingsUpdated()
    }

    private fun recordSnapshotInternal(content: String, author: String, message: String) {
        val snapshot = AgentHistorySnapshot.create(content, author, message)
        myState.history.add(0, snapshot)

        // Trim history to limit
        while (myState.history.size > myState.historyLimit) {
            myState.history.removeAt(myState.history.size - 1)
        }
    }

    private fun notifyContentUpdated(newContent: String, author: String, message: String) {
        val app = ApplicationManager.getApplication()
        if (app != null && !app.isDisposed) {
            app.messageBus.syncPublisher(AgentSyncListener.TOPIC).onContentUpdated(newContent, author, message)
        }
    }

    fun notifySettingsUpdated() {
        val app = ApplicationManager.getApplication()
        if (app != null && !app.isDisposed) {
            app.messageBus.syncPublisher(AgentSyncListener.TOPIC).onSettingsUpdated()
        }
    }
}
