package com.codenote.action

import com.codenote.service.ImportExportService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * 导入笔记Action
 */
class ImportNotesAction : AnAction("导入笔记", "从JSON文件导入代码笔记", AllIcons.ToolbarDecorator.Import) {
    
    private val importExportService = ImportExportService.getInstance()
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // 显示导入模式选择对话框
        showImportDialog(project)
    }
    
    private fun showImportDialog(project: Project) {
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
                importExportService.importNotes(project, ImportExportService.ImportMode.MERGE)
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
                    importExportService.importNotes(project, ImportExportService.ImportMode.REPLACE)
                }
            }
            2 -> {
                // 全部导入
                importExportService.importNotes(project, ImportExportService.ImportMode.ADD_ALL)
            }
            // 3 或其他值表示取消
        }
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}