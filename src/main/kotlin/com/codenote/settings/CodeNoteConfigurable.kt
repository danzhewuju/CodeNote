package com.codenote.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * CodeNote插件配置界面
 */
class CodeNoteConfigurable : Configurable {
    
    private var settingsComponent: CodeNoteSettingsComponent? = null
    
    override fun getDisplayName(): String {
        return "CodeNote"
    }
    
    override fun createComponent(): JComponent? {
        settingsComponent = CodeNoteSettingsComponent()
        return settingsComponent?.getPanel()
    }
    
    override fun isModified(): Boolean {
        val settings = CodeNoteSettings.getInstance()
        val component = settingsComponent ?: return false
        
        return component.getStoragePath() != settings.storagePath ||
               component.getMaxNotesPerProject() != settings.maxNotesPerProject ||
               component.isAutoSave() != settings.autoSave ||
               component.isShowLineNumbers() != settings.showLineNumbers ||
               component.isEnableSyntaxHighlight() != settings.enableSyntaxHighlight
    }
    
    override fun apply() {
        val settings = CodeNoteSettings.getInstance()
        val component = settingsComponent ?: return
        
        settings.storagePath = component.getStoragePath()
        settings.maxNotesPerProject = component.getMaxNotesPerProject()
        settings.autoSave = component.isAutoSave()
        settings.showLineNumbers = component.isShowLineNumbers()
        settings.enableSyntaxHighlight = component.isEnableSyntaxHighlight()
    }
    
    override fun reset() {
        val settings = CodeNoteSettings.getInstance()
        val component = settingsComponent ?: return
        
        component.setStoragePath(settings.storagePath)
        component.setMaxNotesPerProject(settings.maxNotesPerProject)
        component.setAutoSave(settings.autoSave)
        component.setShowLineNumbers(settings.showLineNumbers)
        component.setEnableSyntaxHighlight(settings.enableSyntaxHighlight)
    }
    
    override fun disposeUIResources() {
        settingsComponent = null
    }
    
    /**
     * 设置组件
     */
    private class CodeNoteSettingsComponent {
        private val mainPanel: JPanel = JPanel(BorderLayout())
        private val storagePathField: TextFieldWithBrowseButton = TextFieldWithBrowseButton()
        private val maxNotesField: JBTextField = JBTextField()
        private val autoSaveCheckbox: JBCheckBox = JBCheckBox("自动保存")
        private val showLineNumbersCheckbox: JBCheckBox = JBCheckBox("显示行号")
        private val enableSyntaxHighlightCheckbox: JBCheckBox = JBCheckBox("启用语法高亮")
        
        init {
            // 设置存储路径选择器
            storagePathField.addBrowseFolderListener(
                TextBrowseFolderListener(
                    FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("选择代码笔记存储目录")
                        .withDescription("选择用于存储代码笔记的目录")
                )
            )
            
            // 设置最大笔记数量字段
            maxNotesField.text = "1000"
            
            // 构建表单
            val formBuilder = FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("存储路径:"), storagePathField, 1, false)
                .addLabeledComponent(JBLabel("每个项目最大笔记数:"), maxNotesField, 1, false)
                .addComponent(autoSaveCheckbox, 1)
                .addComponent(showLineNumbersCheckbox, 1)
                .addComponent(enableSyntaxHighlightCheckbox, 1)
                .addComponentFillVertically(JPanel(), 0)
            
            mainPanel.add(formBuilder.panel, BorderLayout.CENTER)
        }
        
        fun getPanel(): JPanel = mainPanel
        
        fun getStoragePath(): String = storagePathField.text.trim()
        fun setStoragePath(path: String) {
            storagePathField.text = path
        }
        
        fun getMaxNotesPerProject(): Int {
            return try {
                maxNotesField.text.toInt().coerceAtLeast(1)
            } catch (e: NumberFormatException) {
                1000
            }
        }
        fun setMaxNotesPerProject(count: Int) {
            maxNotesField.text = count.toString()
        }
        
        fun isAutoSave(): Boolean = autoSaveCheckbox.isSelected
        fun setAutoSave(enabled: Boolean) {
            autoSaveCheckbox.isSelected = enabled
        }
        
        fun isShowLineNumbers(): Boolean = showLineNumbersCheckbox.isSelected
        fun setShowLineNumbers(enabled: Boolean) {
            showLineNumbersCheckbox.isSelected = enabled
        }
        
        fun isEnableSyntaxHighlight(): Boolean = enableSyntaxHighlightCheckbox.isSelected
        fun setEnableSyntaxHighlight(enabled: Boolean) {
            enableSyntaxHighlightCheckbox.isSelected = enabled
        }
    }
}