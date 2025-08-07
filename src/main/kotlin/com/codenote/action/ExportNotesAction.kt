package com.codenote.action

import com.codenote.service.ImportExportService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * 导出笔记Action
 */
class ExportNotesAction : AnAction("导出笔记", "导出代码笔记到JSON文件", AllIcons.ToolbarDecorator.Export) {
    
    private val importExportService = ImportExportService.getInstance()
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // 显示导出选项对话框
        showExportDialog(project)
    }
    
    private fun showExportDialog(project: Project) {
        val options = arrayOf(
            "所有笔记",
            "当前项目笔记",
            "取消"
        )
        
        val choice = Messages.showDialog(
            project,
            "请选择要导出的笔记范围:",
            "导出笔记",
            options,
            0,
            Messages.getQuestionIcon()
        )
        
        when (choice) {
            0 -> {
                // 导出所有笔记
                importExportService.exportNotes(project, ImportExportService.ExportType.ALL_NOTES)
            }
            1 -> {
                // 导出当前项目笔记
                if (project.basePath != null) {
                    importExportService.exportNotes(project, ImportExportService.ExportType.CURRENT_PROJECT)
                } else {
                    Messages.showWarningDialog(
                        project,
                        "当前项目没有有效的路径，无法导出项目笔记",
                        "导出警告"
                    )
                }
            }
            // 2 或其他值表示取消
        }
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}