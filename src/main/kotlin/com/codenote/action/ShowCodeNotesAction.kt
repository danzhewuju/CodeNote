package com.codenote.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 切换代码笔记工具窗口的Action
 * 支持打开/关闭切换功能
 */
class ShowCodeNotesAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("CodeNoteEnhanced")
        
        if (toolWindow != null) {
            if (toolWindow.isVisible) {
                // 如果工具窗口当前可见，则隐藏它
                toolWindow.hide(null)
            } else {
                // 如果工具窗口当前不可见，则显示并激活它
                toolWindow.show(null)
                toolWindow.activate(null)
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
        
        // 动态更新Action的文本，显示当前状态
        if (project != null) {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("CodeNoteEnhanced")
            
            if (toolWindow != null && toolWindow.isVisible) {
                e.presentation.text = "隐藏代码笔记"
                e.presentation.description = "隐藏代码笔记工具窗口"
            } else {
                e.presentation.text = "显示代码笔记"
                e.presentation.description = "显示代码笔记工具窗口"
            }
        }
    }
}