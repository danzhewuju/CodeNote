package com.codenote.settings

import com.codenote.service.ShortcutManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
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
               component.isEnableSyntaxHighlight() != settings.enableSyntaxHighlight ||
               component.isCustomShortcutEnabled() != settings.customShortcutEnabled ||
               component.getCustomShortcutKeyStroke() != settings.customShortcutKeyStroke
    }
    
    override fun apply() {
        val settings = CodeNoteSettings.getInstance()
        val component = settingsComponent ?: return
        
        // 验证快捷键格式
        if (component.isCustomShortcutEnabled()) {
            val shortcutText = component.getCustomShortcutKeyStroke()
            if (shortcutText.isNotBlank() && !ShortcutManager.getInstance().validateShortcutText(shortcutText)) {
                Messages.showErrorDialog(
                    "快捷键格式无效。请使用正确的格式，如：ctrl alt S, ctrl shift N 等。",
                    "快捷键配置错误"
                )
                return
            }
        }
        
        settings.storagePath = component.getStoragePath()
        settings.maxNotesPerProject = component.getMaxNotesPerProject()
        settings.autoSave = component.isAutoSave()
        settings.showLineNumbers = component.isShowLineNumbers()
        settings.enableSyntaxHighlight = component.isEnableSyntaxHighlight()
        settings.customShortcutEnabled = component.isCustomShortcutEnabled()
        settings.customShortcutKeyStroke = component.getCustomShortcutKeyStroke()
        
        // 应用快捷键设置
        ShortcutManager.getInstance().applyCustomShortcut()
    }
    
    override fun reset() {
        val settings = CodeNoteSettings.getInstance()
        val component = settingsComponent ?: return
        
        component.setStoragePath(settings.storagePath)
        component.setMaxNotesPerProject(settings.maxNotesPerProject)
        component.setAutoSave(settings.autoSave)
        component.setShowLineNumbers(settings.showLineNumbers)
        component.setEnableSyntaxHighlight(settings.enableSyntaxHighlight)
        component.setCustomShortcutEnabled(settings.customShortcutEnabled)
        component.setCustomShortcutKeyStroke(settings.customShortcutKeyStroke)
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
        private val customShortcutCheckbox: JBCheckBox = JBCheckBox("启用自定义快捷键")
        private val customShortcutField: JBTextField = JBTextField()
        
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
            
            // 设置快捷键字段
            customShortcutField.text = "ctrl alt s"
            customShortcutField.isEnabled = false
            
            // 添加复选框监听器
            customShortcutCheckbox.addActionListener {
                customShortcutField.isEnabled = customShortcutCheckbox.isSelected
            }
            
            // 构建表单
            val formBuilder = FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("存储路径:"), storagePathField, 1, false)
                .addLabeledComponent(JBLabel("每个项目最大笔记数:"), maxNotesField, 1, false)
                .addComponent(autoSaveCheckbox, 1)
                .addComponent(showLineNumbersCheckbox, 1)
                .addComponent(enableSyntaxHighlightCheckbox, 1)
                .addSeparator()
                .addComponent(customShortcutCheckbox, 1)
                .addLabeledComponent(JBLabel("快捷键 (格式: ctrl alt S):"), customShortcutField, 1, false)
                .addComponent(JBLabel("<html><small>支持的修饰键: ctrl, alt, shift, cmd<br/>示例: ctrl alt s, ctrl shift n, alt f1<br/>注意: 使用小写字母，用空格分隔</small></html>"), 1)
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
        
        fun isCustomShortcutEnabled(): Boolean = customShortcutCheckbox.isSelected
        fun setCustomShortcutEnabled(enabled: Boolean) {
            customShortcutCheckbox.isSelected = enabled
            customShortcutField.isEnabled = enabled
        }
        
        fun getCustomShortcutKeyStroke(): String = customShortcutField.text.trim()
        fun setCustomShortcutKeyStroke(keyStroke: String) {
            customShortcutField.text = keyStroke
        }
    }
}