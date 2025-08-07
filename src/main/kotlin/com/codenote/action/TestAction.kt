package com.codenote.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

/**
 * 测试动作，用于验证插件基础功能
 */
class TestAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        Messages.showInfoMessage(
            project,
            "CodeNote 插件正在运行！\n项目: ${project?.name ?: "未知"}",
            "CodeNote 测试"
        )
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
    }
}