package com.codenote.service

import com.codenote.ui.CodeNoteToolWindowFactory
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 代码笔记通知服务
 * 用于处理代码笔记相关的事件通知和UI刷新
 */
@Service(Service.Level.APP)
class CodeNoteNotificationService {
    
    companion object {
        fun getInstance(): CodeNoteNotificationService {
            return com.intellij.openapi.application.ApplicationManager.getApplication()
                .getService(CodeNoteNotificationService::class.java)
        }
    }
    
    /**
     * 通知侧边栏刷新
     */
    fun notifyCodeNoteAdded(project: Project) {
        // 直接通过工厂类刷新工具窗口
        CodeNoteToolWindowFactory.refreshToolWindow(project)
        
        // 如果工具窗口不可见，可选择显示它
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("CodeNoteEnhanced")
        
        // 可选：自动显示工具窗口（如果用户偏好）
        if (toolWindow != null && !toolWindow.isVisible) {
            // 注释掉自动显示，让用户决定是否查看
            // toolWindow.show(null)
        }
    }
    

    
    /**
     * 通知代码笔记更新
     */
    fun notifyCodeNoteUpdated(project: Project) {
        notifyCodeNoteAdded(project) // 使用相同的刷新逻辑
    }
    
    /**
     * 通知代码笔记删除
     */
    fun notifyCodeNoteDeleted(project: Project) {
        notifyCodeNoteAdded(project) // 使用相同的刷新逻辑
    }
}