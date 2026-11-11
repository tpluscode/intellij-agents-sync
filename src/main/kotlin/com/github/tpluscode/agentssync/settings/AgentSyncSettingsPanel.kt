package com.github.tpluscode.agentssync.settings

import com.github.tpluscode.agentssync.model.AgentHistorySnapshot
import com.github.tpluscode.agentssync.model.AgentSymlinkConfig
import com.github.tpluscode.agentssync.model.AgentSyncState
import com.github.tpluscode.agentssync.service.AgentFileManager
import com.github.tpluscode.agentssync.service.AgentSyncSettingsService
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridLayout
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.ListSelectionModel
import javax.swing.SpinnerNumberModel
import javax.swing.table.DefaultTableModel

/**
 * UI Panel for Agent Sync Settings & Editor in IDE Preferences.
 */
class AgentSyncSettingsPanel {

    val mainPanel = JPanel(BorderLayout())

    // Settings fields
    private val filePathField = TextFieldWithBrowseButton()
    private val autoSyncToFileCheckBox = JBCheckBox("Automatically sync state changes to designated shared file", true)
    private val autoSyncFromFileCheckBox = JBCheckBox("Automatically load external file changes into synced state", true)
    private val symlinksEnabledCheckBox = JBCheckBox("Automatically create and maintain symlinks in agent default locations", true)
    private val mcpEnabledCheckBox = JBCheckBox("Enable embedded MCP Server for AI Assistant & AIR", true)
    private val mcpPortSpinner = JSpinner(SpinnerNumberModel(AgentSyncState.DEFAULT_MCP_PORT, 1024, 65535, 1))
    private val historyLimitSpinner = JSpinner(SpinnerNumberModel(100, 10, 1000, 10))

    // Editor field
    private val contentTextArea = JBTextArea(16, 60).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 13)
        margin = JBUI.insets(8)
    }

    // Symlinks Table
    private val symlinksTableModel = object : DefaultTableModel(
        arrayOf("Enabled", "Agent / Tool", "Target Path", "Status", "Description"), 0
    ) {
        override fun getColumnClass(columnIndex: Int): Class<*> {
            return if (columnIndex == 0) java.lang.Boolean::class.java else java.lang.String::class.java
        }

        override fun isCellEditable(row: Int, column: Int): Boolean {
            return column == 0
        }
    }
    private val symlinksTable = JBTable(symlinksTableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        fillsViewportHeight = true
        columnModel.getColumn(0).preferredWidth = 70
        columnModel.getColumn(1).preferredWidth = 140
        columnModel.getColumn(2).preferredWidth = 230
        columnModel.getColumn(3).preferredWidth = 110
        columnModel.getColumn(4).preferredWidth = 260
    }
    private var currentSymlinks: MutableList<AgentSymlinkConfig> = mutableListOf()

    // History Table
    private val historyTableModel = object : DefaultTableModel(
        arrayOf("Date / Time", "Author", "Message", "Snapshot ID"), 0
    ) {
        override fun getColumnClass(columnIndex: Int): Class<*> = java.lang.String::class.java
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val historyTable = JBTable(historyTableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        fillsViewportHeight = true
    }

    private var currentSnapshots: List<AgentHistorySnapshot> = emptyList()

    init {
        buildUi()
    }

    private fun buildUi() {
        mainPanel.layout = BorderLayout(0, 10)

        // Top Configuration Form
        val configForm = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("Designated Storage & Sync")
        }

        val pathRow = JPanel(BorderLayout(8, 0)).apply {
            add(JBLabel("Designated Shared AGENTS.md Path:"), BorderLayout.WEST)
            add(filePathField, BorderLayout.CENTER)
        }
        filePathField.addBrowseFolderListener(
            "Select Shared AGENTS.md File",
            "Choose the designated local file location for global AGENTS.md",
            null,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        )

        val portRow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(mcpEnabledCheckBox)
            add(Box.createHorizontalStrut(16))
            add(JBLabel("MCP Port:"))
            add(mcpPortSpinner)
            add(Box.createHorizontalStrut(16))
            add(JBLabel("History Snapshots Limit:"))
            add(historyLimitSpinner)
        }

        configForm.add(pathRow)
        configForm.add(Box.createVerticalStrut(6))
        configForm.add(autoSyncToFileCheckBox)
        configForm.add(Box.createVerticalStrut(4))
        configForm.add(autoSyncFromFileCheckBox)
        configForm.add(Box.createVerticalStrut(4))
        configForm.add(symlinksEnabledCheckBox)
        configForm.add(Box.createVerticalStrut(6))
        configForm.add(portRow)

        // Tabbed Pane: 1. Content Editor, 2. Agent Symlinks, 3. History & Revisions
        val tabbedPane = JBTabbedPane()

        // Tab 1: Editor Panel
        val editorPanel = JPanel(BorderLayout(0, 8)).apply {
            border = JBUI.Borders.empty(8)
            val editorActions = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                val openEditorTabBtn = JButton("Open in Full Editor Tab").apply {
                    addActionListener {
                        openInEditorTab()
                    }
                }
                val reloadBtn = JButton("Reload from Local File").apply {
                    addActionListener {
                        val reloaded = AgentFileManager.getInstance().reloadFromDisk()
                        if (reloaded) {
                            contentTextArea.text = AgentSyncSettingsService.getInstance().getContent()
                            Messages.showInfoMessage("Reloaded content from local disk file.", "Reload Successful")
                        } else {
                            Messages.showInfoMessage("Local file is up to date or not found.", "Reload")
                        }
                    }
                }
                add(openEditorTabBtn)
                add(reloadBtn)
            }
            add(editorActions, BorderLayout.NORTH)
            add(JBScrollPane(contentTextArea), BorderLayout.CENTER)
        }
        tabbedPane.addTab("AGENTS.md Editor", editorPanel)

        // Tab 2: Agent Symlinks Panel
        val symlinksPanel = JPanel(BorderLayout(0, 8)).apply {
            border = JBUI.Borders.empty(8)
            val topPanel = JPanel(BorderLayout()).apply {
                add(JBLabel("Symlinks mirror the shared AGENTS.md directly into each agent's native configuration path:"), BorderLayout.NORTH)
                val actions = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
                    val updateNowBtn = JButton("Create / Update Symlinks Now").apply {
                        addActionListener {
                            applyTableChangesToCurrentSymlinks()
                            AgentFileManager.getInstance().updateSymlinks()
                            refreshSymlinksTable()
                            Messages.showInfoMessage("Agent symlinks have been refreshed on disk.", "Symlinks Updated")
                        }
                    }
                    val addCustomBtn = JButton("Add Custom Symlink").apply {
                        addActionListener {
                            showAddSymlinkDialog()
                        }
                    }
                    val removeBtn = JButton("Remove Selected").apply {
                        addActionListener {
                            val selectedRow = symlinksTable.selectedRow
                            if (selectedRow >= 0 && selectedRow < currentSymlinks.size) {
                                currentSymlinks.removeAt(selectedRow)
                                refreshSymlinksTable()
                            } else {
                                Messages.showWarningDialog("Please select a symlink row to remove.", "No Selection")
                            }
                        }
                    }
                    val resetDefaultsBtn = JButton("Reset Defaults").apply {
                        addActionListener {
                            val confirm = Messages.showYesNoDialog(
                                "Reset agent symlinks list to standard defaults?",
                                "Reset Defaults",
                                Messages.getQuestionIcon()
                            )
                            if (confirm == Messages.YES) {
                                currentSymlinks = AgentSymlinkConfig.createDefaults()
                                refreshSymlinksTable()
                            }
                        }
                    }
                    add(updateNowBtn)
                    add(addCustomBtn)
                    add(removeBtn)
                    add(resetDefaultsBtn)
                }
                add(actions, BorderLayout.SOUTH)
            }
            add(topPanel, BorderLayout.NORTH)
            add(JBScrollPane(symlinksTable), BorderLayout.CENTER)
        }
        tabbedPane.addTab("Agent Symlinks", symlinksPanel)

        // Tab 3: History & Snapshots Panel
        val historyPanel = JPanel(BorderLayout(0, 8)).apply {
            border = JBUI.Borders.empty(8)
            val historyActions = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                val restoreBtn = JButton("Restore Selected Snapshot").apply {
                    addActionListener {
                        restoreSelectedSnapshot()
                    }
                }
                val viewDiffBtn = JButton("Preview Snapshot Content").apply {
                    addActionListener {
                        viewSelectedSnapshotContent()
                    }
                }
                val clearBtn = JButton("Clear History").apply {
                    addActionListener {
                        val confirm = Messages.showYesNoDialog(
                            "Are you sure you want to clear all history snapshots?",
                            "Clear History",
                            Messages.getQuestionIcon()
                        )
                        if (confirm == Messages.YES) {
                            AgentSyncSettingsService.getInstance().clearHistory()
                            refreshHistoryTable()
                        }
                    }
                }
                add(restoreBtn)
                add(viewDiffBtn)
                add(clearBtn)
            }
            add(historyActions, BorderLayout.NORTH)
            add(JBScrollPane(historyTable), BorderLayout.CENTER)
        }
        tabbedPane.addTab("History & Snapshots", historyPanel)

        mainPanel.add(configForm, BorderLayout.NORTH)
        mainPanel.add(tabbedPane, BorderLayout.CENTER)
    }

    private fun showAddSymlinkDialog() {
        val dialog = AddSymlinkDialog()
        if (dialog.showAndGet()) {
            val name = dialog.nameField.text.trim()
            val targetPath = dialog.pathField.text.trim()
            val desc = dialog.descField.text.trim()
            if (name.isNotEmpty() && targetPath.isNotEmpty()) {
                currentSymlinks.add(
                    AgentSymlinkConfig(
                        id = "custom_" + System.currentTimeMillis(),
                        name = name,
                        targetPath = targetPath,
                        enabled = true,
                        description = if (desc.isEmpty()) "Custom agent symlink" else desc
                    )
                )
                refreshSymlinksTable()
            }
        }
    }

    private class AddSymlinkDialog : DialogWrapper(true) {
        val nameField = JBTextField()
        val pathField = JBTextField()
        val descField = JBTextField()

        init {
            title = "Add Custom Agent Symlink"
            init()
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(GridLayout(3, 2, 4, 8)).apply {
                preferredSize = Dimension(450, 100)
                add(JBLabel("Agent / Tool Name:"))
                add(nameField)
                add(JBLabel("Target Path (e.g. ~/.myagent/rules.md):"))
                add(pathField)
                add(JBLabel("Description:"))
                add(descField)
            }
            return panel
        }
    }

    private fun openInEditorTab() {
        val fileManager = AgentFileManager.getInstance()
        val settings = AgentSyncSettingsService.getInstance()
        val file = fileManager.getLocalFile()

        if (!file.exists()) {
            fileManager.writeFile(file.toPath(), settings.getContent())
        }

        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (virtualFile != null && project != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }

    private fun restoreSelectedSnapshot() {
        val selectedRow = historyTable.selectedRow
        if (selectedRow < 0 || selectedRow >= currentSnapshots.size) {
            Messages.showWarningDialog("Please select a snapshot to restore.", "No Selection")
            return
        }

        val snapshot = currentSnapshots[selectedRow]
        val confirm = Messages.showYesNoDialog(
            "Revert AGENTS.md to snapshot from ${formatDate(snapshot.timestamp)} (${snapshot.message})?",
            "Confirm Restore",
            Messages.getQuestionIcon()
        )
        if (confirm == Messages.YES) {
            AgentSyncSettingsService.getInstance().rollbackToSnapshot(snapshot.id, "History Rollback")
            contentTextArea.text = snapshot.content
            refreshHistoryTable()
            Messages.showInfoMessage("Successfully restored snapshot.", "Rollback Complete")
        }
    }

    private fun viewSelectedSnapshotContent() {
        val selectedRow = historyTable.selectedRow
        if (selectedRow < 0 || selectedRow >= currentSnapshots.size) {
            Messages.showWarningDialog("Please select a snapshot to view.", "No Selection")
            return
        }

        val snapshot = currentSnapshots[selectedRow]
        Messages.showInputDialog(
            null,
            "Snapshot details from ${formatDate(snapshot.timestamp)} by ${snapshot.author}:\n${snapshot.message}",
            "Snapshot Content Viewer",
            null,
            snapshot.content,
            null
        )
    }

    private fun applyTableChangesToCurrentSymlinks() {
        for (row in 0 until symlinksTableModel.rowCount) {
            if (row < currentSymlinks.size) {
                val enabled = symlinksTableModel.getValueAt(row, 0) as? Boolean ?: true
                currentSymlinks[row].enabled = enabled
            }
        }
    }

    fun reset(state: AgentSyncState) {
        filePathField.text = state.customFilePath
        autoSyncToFileCheckBox.isSelected = state.autoSyncToFile
        autoSyncFromFileCheckBox.isSelected = state.autoSyncFromFile
        symlinksEnabledCheckBox.isSelected = state.symlinksEnabled
        mcpEnabledCheckBox.isSelected = state.mcpEnabled
        mcpPortSpinner.value = state.mcpPort
        historyLimitSpinner.value = state.historyLimit
        contentTextArea.text = state.content

        currentSymlinks = state.agentSymlinks.map { it.copy() }.toMutableList()
        refreshSymlinksTable()
        refreshHistoryTable()
    }

    fun apply(state: AgentSyncState) {
        state.customFilePath = filePathField.text.trim()
        state.autoSyncToFile = autoSyncToFileCheckBox.isSelected
        state.autoSyncFromFile = autoSyncFromFileCheckBox.isSelected
        state.symlinksEnabled = symlinksEnabledCheckBox.isSelected
        state.mcpEnabled = mcpEnabledCheckBox.isSelected
        state.mcpPort = mcpPortSpinner.value as Int
        state.historyLimit = historyLimitSpinner.value as Int

        applyTableChangesToCurrentSymlinks()
        state.agentSymlinks = currentSymlinks.map { it.copy() }.toMutableList()

        val newContent = contentTextArea.text
        if (state.content != newContent) {
            AgentSyncSettingsService.getInstance().updateContent(newContent, "Settings Dialog", "Updated in Settings Dialog")
        }

        if (state.symlinksEnabled) {
            AgentFileManager.getInstance().updateSymlinks()
        }
    }

    fun isModified(state: AgentSyncState): Boolean {
        if (filePathField.text.trim() != state.customFilePath ||
            autoSyncToFileCheckBox.isSelected != state.autoSyncToFile ||
            autoSyncFromFileCheckBox.isSelected != state.autoSyncFromFile ||
            symlinksEnabledCheckBox.isSelected != state.symlinksEnabled ||
            mcpEnabledCheckBox.isSelected != state.mcpEnabled ||
            (mcpPortSpinner.value as Int) != state.mcpPort ||
            (historyLimitSpinner.value as Int) != state.historyLimit ||
            contentTextArea.text != state.content
        ) {
            return true
        }

        applyTableChangesToCurrentSymlinks()
        if (currentSymlinks.size != state.agentSymlinks.size) return true
        for (i in currentSymlinks.indices) {
            val a = currentSymlinks[i]
            val b = state.agentSymlinks[i]
            if (a.id != b.id || a.enabled != b.enabled || a.targetPath != b.targetPath || a.name != b.name) {
                return true
            }
        }
        return false
    }

    private fun refreshSymlinksTable() {
        symlinksTableModel.rowCount = 0
        val designatedPath = AgentFileManager.resolvePath(filePathField.text.trim().ifEmpty { AgentSyncState.DEFAULT_FILE_PATH })
        val fileManager = AgentFileManager.getInstance()

        for (symlink in currentSymlinks) {
            val targetPath = AgentFileManager.resolvePath(symlink.targetPath)
            val status = fileManager.getSymlinkStatus(targetPath, designatedPath)
            symlinksTableModel.addRow(
                arrayOf(
                    symlink.enabled,
                    symlink.name,
                    symlink.targetPath,
                    status,
                    symlink.description
                )
            )
        }
    }

    private fun refreshHistoryTable() {
        historyTableModel.rowCount = 0
        currentSnapshots = AgentSyncSettingsService.getInstance().getHistory()

        for (snapshot in currentSnapshots) {
            historyTableModel.addRow(
                arrayOf(
                    formatDate(snapshot.timestamp),
                    snapshot.author,
                    snapshot.message,
                    snapshot.id.take(8)
                )
            )
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(timestamp))
    }
}
