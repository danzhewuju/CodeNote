package com.codenote.ui

import com.codenote.model.CodeNote
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.*
import com.intellij.ui.components.panels.HorizontalBox
import com.intellij.ui.components.panels.VerticalBox
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

/**
 * 添加代码笔记对话框
 */
class AddCodeNoteDialog(
    private val project: Project,
    private val originalCodeNote: CodeNote
) : DialogWrapper(project) {
    
    private lateinit var titleField: JBTextField
    private lateinit var noteField: JBTextArea
    private lateinit var tagsField: JBTextField
    private lateinit var codeEditor: EditorEx
    
    init {
        // 根据是否有现有内容来判断是添加还是编辑
        val isEditing = originalCodeNote.note.isNotBlank() || originalCodeNote.tags.isNotEmpty() || 
                       (originalCodeNote.title.isNotBlank() && originalCodeNote.title != generateDefaultTitle())
        
        title = if (isEditing) "编辑代码笔记" else "添加代码笔记"
        setOKButtonText("保存")
        setCancelButtonText("取消")
        init()
    }
    
    override fun dispose() {
        if (::codeEditor.isInitialized) {
            EditorFactory.getInstance().releaseEditor(codeEditor)
        }
        super.dispose()
    }
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.border = JBUI.Borders.empty(10)
        
        // 创建垂直布局的主容器
        val contentPanel = VerticalBox()
        contentPanel.border = JBUI.Borders.empty()
        
        // 1. 标题区域
        contentPanel.add(createTitleSection())
        contentPanel.add(Box.createVerticalStrut(10))
        
        // 2. 文件信息区域
        contentPanel.add(createFileInfoSection())
        contentPanel.add(Box.createVerticalStrut(15))
        
        // 3. 代码区域（带语法高亮）
        contentPanel.add(createCodeSection())
        contentPanel.add(Box.createVerticalStrut(15))
        
        // 4. 笔记区域
        contentPanel.add(createNoteSection())
        contentPanel.add(Box.createVerticalStrut(10))
        
        // 5. 标签区域
        contentPanel.add(createTagsSection())
        
        mainPanel.add(contentPanel, BorderLayout.CENTER)
        mainPanel.preferredSize = Dimension(750, 600)
        
        return mainPanel
    }
    
    private fun createTitleSection(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = createSectionBorder("📝 笔记标题")
        
        titleField = JBTextField()
        titleField.text = originalCodeNote.title
        titleField.font = titleField.font.deriveFont(Font.BOLD, 14f)
        
        panel.add(titleField, BorderLayout.CENTER)
        return panel
    }
    
    private fun createFileInfoSection(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = createSectionBorder("📁 文件信息")
        
        val fileInfo = HorizontalBox()
        
        // 文件图标
        val fileIcon = JBLabel(AllIcons.FileTypes.Java) // 可以根据文件类型动态设置
        fileInfo.add(fileIcon)
        fileInfo.add(Box.createHorizontalStrut(8))
        
        // 文件路径
        val pathLabel = JBLabel("${originalCodeNote.getRelativePath()}")
        pathLabel.font = pathLabel.font.deriveFont(Font.BOLD)
        fileInfo.add(pathLabel)
        
        fileInfo.add(Box.createHorizontalGlue())
        
        // 行号信息
        val lineInfo = JBLabel("行 ${originalCodeNote.startLine}-${originalCodeNote.endLine}")
        lineInfo.foreground = JBColor.GRAY
        fileInfo.add(lineInfo)
        
        panel.add(fileInfo, BorderLayout.CENTER)
        return panel
    }
    
    private fun createCodeSection(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = createSectionBorder("💻 代码片段")
        
        // 创建带语法高亮的编辑器
        val document = EditorFactory.getInstance().createDocument(originalCodeNote.content)
        
        // 根据文件扩展名确定文件类型
        val fileName = originalCodeNote.filePath.substringAfterLast("/")
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName)
        
        codeEditor = EditorFactory.getInstance().createEditor(document, project, fileType, true) as EditorEx
        
        // 配置编辑器设置
        val settings = codeEditor.settings
        settings.isLineNumbersShown = true
        settings.isLineMarkerAreaShown = false
        settings.isFoldingOutlineShown = false
        settings.isRightMarginShown = false
        settings.isWhitespacesShown = false
        settings.isIndentGuidesShown = true
        
        // 设置编辑器为只读
        codeEditor.document.setReadOnly(true)
        
        // 设置编辑器大小
        codeEditor.component.preferredSize = Dimension(700, 250)
        codeEditor.component.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.GRAY, 1),
            JBUI.Borders.empty(5)
        )
        
        panel.add(codeEditor.component, BorderLayout.CENTER)
        return panel
    }
    
    private fun createNoteSection(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = createSectionBorder("📄 笔记内容")
        
        noteField = JBTextArea()
        noteField.text = originalCodeNote.note
        noteField.lineWrap = true
        noteField.wrapStyleWord = true
        noteField.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        noteField.background = UIUtil.getTextFieldBackground()
        noteField.toolTipText = "在此处添加您的笔记和说明"
        
        val noteScrollPane = JBScrollPane(noteField)
        noteScrollPane.preferredSize = Dimension(700, 120)
        noteScrollPane.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.GRAY, 1),
            JBUI.Borders.empty(5)
        )
        
        panel.add(noteScrollPane, BorderLayout.CENTER)
        return panel
    }
    
    private fun createTagsSection(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = createSectionBorder("🏷️ 标签")
        
        val tagsPanel = HorizontalBox()
        
        tagsField = JBTextField()
        tagsField.text = originalCodeNote.tags.joinToString(", ")
        tagsField.toolTipText = "用逗号分隔多个标签，例如：算法,优化,重要"
        
        val exampleLabel = JBLabel("例如：算法, 优化, 重要")
        exampleLabel.foreground = JBColor.GRAY
        exampleLabel.font = exampleLabel.font.deriveFont(Font.ITALIC, 11f)
        
        tagsPanel.add(tagsField)
        tagsPanel.add(Box.createHorizontalStrut(10))
        tagsPanel.add(exampleLabel)
        
        panel.add(tagsPanel, BorderLayout.CENTER)
        return panel
    }
    
    private fun createSectionBorder(title: String): CompoundBorder {
        val titledBorder = TitledBorder(title)
        titledBorder.titleFont = titledBorder.titleFont.deriveFont(Font.BOLD, 12f)
        titledBorder.titleColor = JBColor.DARK_GRAY
        
        return CompoundBorder(
            titledBorder,
            EmptyBorder(8, 8, 8, 8)
        )
    }
    
    /**
     * 生成默认标题
     */
    private fun generateDefaultTitle(): String {
        val fileName = originalCodeNote.filePath.substringAfterLast("/")
        return "$fileName"
    }
    
    /**
     * 获取用户输入的代码笔记
     */
    fun getCodeNote(): CodeNote {
        val tags = tagsField.text.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        return originalCodeNote.copy(
            title = titleField.text.trim(),
            note = noteField.text.trim(),
            tags = tags
        )
    }
    
    override fun getPreferredFocusedComponent(): JComponent {
        return titleField
    }
}