package com.codenote.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * 代码笔记工具窗口工厂
 */
class CodeNoteToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val codeNoteToolWindow = CodeNoteToolWindow(project)
        val content = ContentFactory.SERVICE.getInstance().createContent(
            codeNoteToolWindow.getContent(),
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
    }
}