package com.codenote.service

import com.codenote.settings.CodeNoteSettings
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.components.Service
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManager
import javax.swing.KeyStroke

/**
 * 快捷键管理服务
 */
@Service(Service.Level.APP)
class ShortcutManager {
    
    companion object {
        private const val SHOW_CODE_NOTES_ACTION_ID = "ShowCodeNotes"
        private const val SHOW_CODE_NOTES_FROM_MENU_ACTION_ID = "ShowCodeNotesFromMenu"
        
        fun getInstance(): ShortcutManager {
            return com.intellij.openapi.application.ApplicationManager.getApplication()
                .getService(ShortcutManager::class.java)
        }
    }
    
    /**
     * 应用自定义快捷键设置
     */
    fun applyCustomShortcut() {
        val settings = CodeNoteSettings.getInstance()
        val keymapManager = KeymapManager.getInstance()
        val activeKeymap = keymapManager.activeKeymap
        
        if (settings.customShortcutEnabled && settings.customShortcutKeyStroke.isNotBlank()) {
            try {
                // 解析快捷键字符串
                val keyStroke = parseKeyStroke(settings.customShortcutKeyStroke)
                if (keyStroke != null) {
                    val shortcut = KeyboardShortcut(keyStroke, null)
                    
                    // 移除现有的快捷键
                    removeExistingShortcuts(activeKeymap)
                    
                    // 添加新的快捷键
                    activeKeymap.addShortcut(SHOW_CODE_NOTES_ACTION_ID, shortcut)
                    activeKeymap.addShortcut(SHOW_CODE_NOTES_FROM_MENU_ACTION_ID, shortcut)
                }
            } catch (e: Exception) {
                // 如果解析失败，使用默认快捷键
                resetToDefaultShortcut(activeKeymap)
            }
        } else {
            // 如果禁用自定义快捷键，使用默认快捷键
            resetToDefaultShortcut(activeKeymap)
        }
    }
    
    /**
     * 解析快捷键字符串
     */
    private fun parseKeyStroke(shortcutText: String): KeyStroke? {
        return try {
            // 将用户输入的格式转换为KeyStroke可识别的格式
            val parts = shortcutText.trim().toLowerCase().split(" ")
            val modifiers = mutableListOf<String>()
            var key = ""
            
            for (part in parts) {
                when (part) {
                    "ctrl" -> modifiers.add("control")
                    "cmd", "win" -> modifiers.add("meta")
                    "alt" -> modifiers.add("alt")
                    "shift" -> modifiers.add("shift")
                    else -> key = part.toUpperCase()
                }
            }
            
            // 构建正确的KeyStroke字符串格式
            val keystrokeString = if (modifiers.isNotEmpty()) {
                "${modifiers.joinToString(" ")} $key"
            } else {
                key
            }
            
            KeyStroke.getKeyStroke(keystrokeString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 移除现有的快捷键
     */
    private fun removeExistingShortcuts(keymap: Keymap) {
        val actionManager = ActionManager.getInstance()
        val showNotesAction = actionManager.getAction(SHOW_CODE_NOTES_ACTION_ID)
        val showNotesFromMenuAction = actionManager.getAction(SHOW_CODE_NOTES_FROM_MENU_ACTION_ID)
        
        if (showNotesAction != null) {
            keymap.getShortcuts(SHOW_CODE_NOTES_ACTION_ID).forEach { shortcut ->
                keymap.removeShortcut(SHOW_CODE_NOTES_ACTION_ID, shortcut)
            }
        }
        
        if (showNotesFromMenuAction != null) {
            keymap.getShortcuts(SHOW_CODE_NOTES_FROM_MENU_ACTION_ID).forEach { shortcut ->
                keymap.removeShortcut(SHOW_CODE_NOTES_FROM_MENU_ACTION_ID, shortcut)
            }
        }
    }
    
    /**
     * 重置为默认快捷键
     */
    private fun resetToDefaultShortcut(keymap: Keymap) {
        try {
            val defaultKeyStroke = KeyStroke.getKeyStroke("control alt S")
            if (defaultKeyStroke != null) {
                val defaultShortcut = KeyboardShortcut(defaultKeyStroke, null)
                
                // 移除现有快捷键
                removeExistingShortcuts(keymap)
                
                // 添加默认快捷键
                keymap.addShortcut(SHOW_CODE_NOTES_ACTION_ID, defaultShortcut)
                keymap.addShortcut(SHOW_CODE_NOTES_FROM_MENU_ACTION_ID, defaultShortcut)
            }
        } catch (e: Exception) {
            // 忽略错误，保持现有配置
        }
    }
    
    /**
     * 验证快捷键字符串格式
     */
    fun validateShortcutText(shortcutText: String): Boolean {
        if (shortcutText.isBlank()) return false
        
        val parts = shortcutText.trim().toLowerCase().split(" ")
        if (parts.isEmpty()) return false
        
        // 检查是否至少有一个按键
        var hasKey = false
        val validModifiers = setOf("ctrl", "alt", "shift", "cmd", "win", "meta")
        
        for (part in parts) {
            if (part in validModifiers) {
                continue
            } else if (part.length == 1 && (part[0].isLetterOrDigit())) {
                hasKey = true
            } else if (part.matches(Regex("f\\d{1,2}"))) { // F1-F12
                hasKey = true
            } else {
                return false // 无效的部分
            }
        }
        
        return hasKey && parseKeyStroke(shortcutText) != null
    }
    
    /**
     * 获取当前快捷键描述
     */
    fun getCurrentShortcutDescription(): String {
        val settings = CodeNoteSettings.getInstance()
        return if (settings.customShortcutEnabled && settings.customShortcutKeyStroke.isNotBlank()) {
            "${settings.customShortcutKeyStroke} (切换显示/隐藏)"
        } else {
            "Ctrl+Alt+S (默认，切换显示/隐藏)"
        }
    }
}