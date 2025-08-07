package com.codenote.ui

import com.codenote.model.CodeNote
import com.codenote.service.CodeNoteService
import com.codenote.service.ImportExportService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.*
import com.intellij.ui.components.panels.HorizontalBox
import com.intellij.ui.components.panels.VerticalBox
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

/**
 * 代码笔记工具窗口
 */
class CodeNoteToolWindow(private val project: Project) {
    
    private val codeNoteService = CodeNoteService.getInstance()
    private val importExportService = ImportExportService.getInstance()
    private val searchField = SearchTextField()
    private val projectComboBox = JComboBox<String>()
    private val notesList = JBList<CodeNote>()
    private val listModel = DefaultListModel<CodeNote>()
    private val statusLabel = JBLabel()
    private var isUpdatingComboBox = false
    
    init {
        setupUI()
        loadNotes()
    }
    
    fun getContent(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.background = UIUtil.getListBackground()
        mainPanel.border = JBUI.Borders.empty(8)
        
        // 创建头部区域
        val headerPanel = createHeaderPanel()
        mainPanel.add(headerPanel, BorderLayout.NORTH)
        
        // 创建内容区域
        val contentPanel = createContentPanel()
        mainPanel.add(contentPanel, BorderLayout.CENTER)
        
        // 创建状态栏
        val statusPanel = createStatusPanel()
        mainPanel.add(statusPanel, BorderLayout.SOUTH)
        
        return mainPanel
    }
    
    private fun createHeaderPanel(): JComponent {
        val headerPanel = VerticalBox()
        headerPanel.border = JBUI.Borders.empty(0, 0, 10, 0)
        
        // 标题区域
        val titlePanel = HorizontalBox()
        val titleIcon = JBLabel(AllIcons.FileTypes.Text)
        val titleLabel = JBLabel("代码笔记")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 14f)
        titlePanel.add(titleIcon)
        titlePanel.add(Box.createHorizontalStrut(5))
        titlePanel.add(titleLabel)
        titlePanel.add(Box.createHorizontalGlue())
        
        // 工具栏
        val toolBar = createToolBar()
        titlePanel.add(toolBar)
        
        headerPanel.add(titlePanel)
        headerPanel.add(Box.createVerticalStrut(8))
        
        // 项目选择器
        val projectPanel = createProjectSelectorPanel()
        headerPanel.add(projectPanel)
        headerPanel.add(Box.createVerticalStrut(8))
        
        // 搜索框
        val searchPanel = createSearchPanel()
        headerPanel.add(searchPanel)
        
        return headerPanel
    }
    
    private fun createProjectSelectorPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = createSectionBorder("📁 项目筛选")
        
        projectComboBox.toolTipText = "选择要查看的项目"
        projectComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, 
                isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is String) {
                    when (value) {
                        "所有项目" -> icon = AllIcons.Nodes.ModuleGroup
                        "当前项目" -> icon = AllIcons.Nodes.Module
                        else -> icon = AllIcons.Nodes.Project
                    }
                }
                return component
            }
        }
        
        projectComboBox.addActionListener {
            if (!isUpdatingComboBox) {
                loadNotes()
            }
        }
        
        panel.add(projectComboBox, BorderLayout.CENTER)
        return panel
    }
    
    private fun createSearchPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = createSectionBorder("🔍 搜索")
        
        searchField.toolTipText = "搜索标题、内容、标签或文件路径..."
        searchField.addDocumentListener(object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
                filterNotes(searchField.text)
            }
        })
        
        panel.add(searchField, BorderLayout.CENTER)
        return panel
    }
    
    private fun createContentPanel(): JComponent {
        val contentPanel = JPanel(BorderLayout())
        
        // 笔记列表
        notesList.model = listModel
        notesList.cellRenderer = CodeNoteListCellRenderer()
        notesList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        notesList.background = UIUtil.getListBackground()
        
        // 设置列表样式
        notesList.border = JBUI.Borders.empty(5)
        
        // 双击跳转
        notesList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedNote = notesList.selectedValue
                    if (selectedNote != null) {
                        codeNoteService.navigateToCode(project, selectedNote)
                    }
                }
            }
        })
        
        // 右键菜单
        notesList.componentPopupMenu = createPopupMenu()
        
        // 空状态显示
        notesList.emptyText.text = "暂无代码笔记"
        notesList.emptyText.appendLine("右键代码选择「添加代码笔记」来创建")
        
        val scrollPane = JBScrollPane(notesList)
        scrollPane.border = JBUI.Borders.compound(
            createSectionBorder("📝 笔记列表"),
            JBUI.Borders.empty()
        )
        
        contentPanel.add(scrollPane, BorderLayout.CENTER)
        return contentPanel
    }
    
    private fun createStatusPanel(): JComponent {
        val statusPanel = JPanel(BorderLayout())
        statusPanel.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.GRAY, 1, 0, 0, 0),
            JBUI.Borders.empty(5, 8, 5, 8)
        )
        statusPanel.background = UIUtil.getPanelBackground()
        
        statusLabel.foreground = JBColor.GRAY
        statusLabel.font = statusLabel.font.deriveFont(11f)
        updateStatusLabel()
        
        statusPanel.add(statusLabel, BorderLayout.WEST)
        
        // 添加快捷键提示
        val helpLabel = JBLabel("双击跳转 | 右键菜单")
        helpLabel.foreground = JBColor.GRAY
        helpLabel.font = helpLabel.font.deriveFont(Font.ITALIC, 10f)
        statusPanel.add(helpLabel, BorderLayout.EAST)
        
        return statusPanel
    }
    
    private fun createSectionBorder(title: String): CompoundBorder {
        val titledBorder = TitledBorder(title)
        titledBorder.titleFont = titledBorder.titleFont.deriveFont(Font.BOLD, 11f)
        titledBorder.titleColor = JBColor.DARK_GRAY
        
        return CompoundBorder(
            titledBorder,
            EmptyBorder(5, 5, 5, 5)
        )
    }
    
    private fun updateStatusLabel() {
        val totalNotes = codeNoteService.getAllCodeNotes().size
        val displayedNotes = listModel.size()
        
        statusLabel.text = when {
            totalNotes == 0 -> "无笔记"
            displayedNotes == totalNotes -> "共 $totalNotes 条笔记"
            else -> "显示 $displayedNotes / $totalNotes 条笔记"
        }
    }
    
    private fun setupUI() {
        // UI设置已移动到各个创建方法中
    }
    
    private fun createToolBar(): JComponent {
        val actionGroup = DefaultActionGroup()
        
        // 刷新按钮
        actionGroup.add(object : AnAction("刷新", "刷新代码笔记列表", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                loadNotes()
                Messages.showInfoMessage(project, "笔记列表已刷新", "CodeNote")
            }
        })
        
        // 删除按钮
        actionGroup.add(object : AnAction("删除", "删除选中的代码笔记", AllIcons.Actions.Cancel) {
            override fun actionPerformed(e: AnActionEvent) {
                deleteSelectedNote()
            }
            
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = notesList.selectedValue != null
            }
        })
        
        // 清空搜索按钮
        actionGroup.add(object : AnAction("清空搜索", "清空搜索条件", AllIcons.Actions.Close) {
            override fun actionPerformed(e: AnActionEvent) {
                searchField.text = ""
                loadNotes()
            }
            
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = searchField.text.isNotEmpty()
            }
        })
        
        actionGroup.addSeparator()
        
        // 导出按钮
        actionGroup.add(object : AnAction("导出笔记", "导出代码笔记到JSON文件", AllIcons.ToolbarDecorator.Export) {
            override fun actionPerformed(e: AnActionEvent) {
                showExportDialog()
            }
        })
        
        // 导入按钮
        actionGroup.add(object : AnAction("导入笔记", "从JSON文件导入代码笔记", AllIcons.ToolbarDecorator.Import) {
            override fun actionPerformed(e: AnActionEvent) {
                showImportDialog()
            }
        })
        
        val toolBar = ActionManager.getInstance().createActionToolbar(
            "CodeNoteToolWindow",
            actionGroup,
            true
        )
        toolBar.targetComponent = notesList
        return toolBar.component
    }
    
    private fun createPopupMenu(): JPopupMenu {
        val popupMenu = JPopupMenu()
        
        val jumpToCodeItem = JMenuItem("🔗 跳转到代码", AllIcons.Actions.EditSource)
        jumpToCodeItem.addActionListener {
            val selectedNote = notesList.selectedValue
            if (selectedNote != null) {
                codeNoteService.navigateToCode(project, selectedNote)
            }
        }
        popupMenu.add(jumpToCodeItem)
        
        popupMenu.addSeparator()
        
        val copyPathItem = JMenuItem("📋 复制文件路径", AllIcons.Actions.Copy)
        copyPathItem.addActionListener {
            val selectedNote = notesList.selectedValue
            if (selectedNote != null) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(java.awt.datatransfer.StringSelection(selectedNote.filePath), null)
                Messages.showInfoMessage(project, "文件路径已复制到剪贴板", "CodeNote")
            }
        }
        popupMenu.add(copyPathItem)
        
        val copyContentItem = JMenuItem("📄 复制代码内容", AllIcons.Actions.Copy)
        copyContentItem.addActionListener {
            val selectedNote = notesList.selectedValue
            if (selectedNote != null) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(java.awt.datatransfer.StringSelection(selectedNote.content), null)
                Messages.showInfoMessage(project, "代码内容已复制到剪贴板", "CodeNote")
            }
        }
        popupMenu.add(copyContentItem)
        
        popupMenu.addSeparator()
        
        val deleteItem = JMenuItem("🗑️ 删除", AllIcons.Actions.Cancel)
        deleteItem.addActionListener {
            deleteSelectedNote()
        }
        popupMenu.add(deleteItem)
        
        return popupMenu
    }
    
    /**
     * 公共刷新方法，供外部调用
     */
    fun refreshNotes() {
        loadNotes()
    }
    
    private fun loadNotes() {
        SwingUtilities.invokeLater {
            // 更新项目下拉框
            updateProjectComboBox()
            
            // 加载笔记
            listModel.clear()
            val selectedProject = projectComboBox.selectedItem as? String
            val notes = when {
                selectedProject == null || selectedProject == "所有项目" -> {
                    codeNoteService.getAllCodeNotes()
                }
                selectedProject == "当前项目" && project.basePath != null -> {
                    codeNoteService.getCodeNotesByProject(project.basePath!!)
                }
                else -> {
                    // 根据项目名称查找对应的项目路径
                    val allProjects = codeNoteService.getAllProjects()
                    val targetProject = allProjects.find { File(it).name == selectedProject }
                    if (targetProject != null) {
                        codeNoteService.getCodeNotesByProject(targetProject)
                    } else {
                        emptyList()
                    }
                }
            }
            notes.forEach { listModel.addElement(it) }
            updateStatusLabel()
        }
    }
    
    private fun updateProjectComboBox() {
        isUpdatingComboBox = true
        try {
            val currentSelection = projectComboBox.selectedItem
            projectComboBox.removeAllItems()
            
            // 添加默认选项
            projectComboBox.addItem("所有项目")
            if (project.basePath != null) {
                projectComboBox.addItem("当前项目")
            }
            
            // 添加所有项目
            val allProjects = codeNoteService.getAllProjects()
            allProjects.forEach { projectPath ->
                val projectName = File(projectPath).name
                projectComboBox.addItem(projectName)
            }
            
            // 恢复之前的选择，或默认选择当前项目
            if (currentSelection != null && (0 until projectComboBox.itemCount).any { 
                    projectComboBox.getItemAt(it) == currentSelection 
                }) {
                projectComboBox.selectedItem = currentSelection
            } else if (project.basePath != null) {
                projectComboBox.selectedItem = "当前项目"
            }
        } finally {
            isUpdatingComboBox = false
        }
    }
    
    private fun filterNotes(query: String) {
        SwingUtilities.invokeLater {
            listModel.clear()
            val selectedProject = projectComboBox.selectedItem as? String
            
            val baseNotes = when {
                selectedProject == null || selectedProject == "所有项目" -> {
                    codeNoteService.getAllCodeNotes()
                }
                selectedProject == "当前项目" && project.basePath != null -> {
                    codeNoteService.getCodeNotesByProject(project.basePath!!)
                }
                else -> {
                    val allProjects = codeNoteService.getAllProjects()
                    val targetProject = allProjects.find { File(it).name == selectedProject }
                    if (targetProject != null) {
                        codeNoteService.getCodeNotesByProject(targetProject)
                    } else {
                        emptyList()
                    }
                }
            }
            
            val filteredNotes = if (query.isBlank()) {
                baseNotes
            } else {
                baseNotes.filter { note ->
                    note.title.lowercase().contains(query.lowercase()) ||
                    note.content.lowercase().contains(query.lowercase()) ||
                    note.note.lowercase().contains(query.lowercase()) ||
                    note.tags.any { it.lowercase().contains(query.lowercase()) } ||
                    note.filePath.lowercase().contains(query.lowercase())
                }
            }
            filteredNotes.forEach { listModel.addElement(it) }
            updateStatusLabel()
        }
    }
    
    private fun deleteSelectedNote() {
        val selectedNote = notesList.selectedValue ?: return
        
        val result = Messages.showYesNoDialog(
            project,
            "确定要删除这条代码笔记吗？",
            "确认删除",
            Messages.getQuestionIcon()
        )
        
        if (result == Messages.YES) {
            codeNoteService.deleteCodeNote(selectedNote.id)
            loadNotes()
        }
    }
    
    private fun showExportDialog() {
        val options = arrayOf(
            "所有笔记",
            "当前项目笔记",
            "取消"
        )
        
        val choice = Messages.showDialog(
            project,
            "请选择要导出的笔记范围:",
            "导出笔记",
            options,
            0,
            Messages.getQuestionIcon()
        )
        
        when (choice) {
            0 -> {
                // 导出所有笔记
                val success = importExportService.exportNotes(project, ImportExportService.ExportType.ALL_NOTES)
                if (success) {
                    loadNotes() // 刷新列表
                }
            }
            1 -> {
                // 导出当前项目笔记
                if (project.basePath != null) {
                    val success = importExportService.exportNotes(project, ImportExportService.ExportType.CURRENT_PROJECT)
                    if (success) {
                        loadNotes() // 刷新列表
                    }
                } else {
                    Messages.showWarningDialog(
                        project,
                        "当前项目没有有效的路径，无法导出项目笔记",
                        "导出警告"
                    )
                }
            }
        }
    }
    
    private fun showImportDialog() {
        val options = arrayOf(
            "合并导入 (跳过重复)",
            "替换导入 (清空现有)",
            "全部导入 (重复生成新ID)",
            "取消"
        )
        
        val descriptions = arrayOf(
            "保留现有笔记，只导入不重复的新笔记",
            "清空所有现有笔记，然后导入新笔记",
            "导入所有笔记，重复的笔记会生成新的ID",
            ""
        )
        
        val message = buildString {
            append("请选择导入模式:\n\n")
            for (i in 0 until options.size - 1) {
                append("${i + 1}. ${options[i]}\n")
                append("   ${descriptions[i]}\n\n")
            }
        }
        
        val choice = Messages.showDialog(
            project,
            message,
            "导入笔记",
            options,
            0,
            Messages.getQuestionIcon()
        )
        
        when (choice) {
            0 -> {
                // 合并导入
                val success = importExportService.importNotes(project, ImportExportService.ImportMode.MERGE)
                if (success) {
                    loadNotes() // 刷新列表
                }
            }
            1 -> {
                // 替换导入
                val confirmResult = Messages.showYesNoDialog(
                    project,
                    "替换导入将删除所有现有笔记！\n这个操作无法撤销，确定要继续吗？",
                    "确认替换导入",
                    "确定",
                    "取消",
                    Messages.getWarningIcon()
                )
                
                if (confirmResult == Messages.YES) {
                    val success = importExportService.importNotes(project, ImportExportService.ImportMode.REPLACE)
                    if (success) {
                        loadNotes() // 刷新列表
                    }
                }
            }
            2 -> {
                // 全部导入
                val success = importExportService.importNotes(project, ImportExportService.ImportMode.ADD_ALL)
                if (success) {
                    loadNotes() // 刷新列表
                }
            }
        }
    }
}