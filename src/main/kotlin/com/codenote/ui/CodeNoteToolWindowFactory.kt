package com.codenote.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 代码笔记工具窗口工厂
 */
class CodeNoteToolWindowFactory : ToolWindowFactory {
    
    companion object {
        // 存储项目到工具窗口实例的映射
        private val toolWindowInstances = ConcurrentHashMap<String, CodeNoteToolWindow>()
        
        /**
         * 获取指定项目的工具窗口实例
         */
        fun getToolWindowInstance(project: Project): CodeNoteToolWindow? {
            return toolWindowInstances[project.basePath ?: project.name]
        }
        
        /**
         * 刷新指定项目的工具窗口
         */
        fun refreshToolWindow(project: Project) {
            getToolWindowInstance(project)?.refreshNotes()
        }
    }
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val codeNoteToolWindow = CodeNoteToolWindow(project)
        
        // 存储工具窗口实例
        toolWindowInstances[project.basePath ?: project.name] = codeNoteToolWindow
        
        val content = ContentFactory.SERVICE.getInstance().createContent(
            codeNoteToolWindow.getContent(),
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
        
        // 当工具窗口关闭时清理实例
        toolWindow.contentManager.addContentManagerListener(object : com.intellij.ui.content.ContentManagerListener {
            override fun contentRemoved(event: com.intellij.ui.content.ContentManagerEvent) {
                toolWindowInstances.remove(project.basePath ?: project.name)
            }
        })
    }
}