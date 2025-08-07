package com.codenote.action

import com.codenote.model.CodeNote
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * 跳转到代码位置的动作
 */
class JumpToCodeAction(
    private val codeNote: CodeNote
) : AnAction("跳转到代码", "跳转到代码位置: ${codeNote.getLocationInfo()}", null) {
    
    private val logger = thisLogger()
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            jumpToCode(project, codeNote)
        } catch (ex: Exception) {
            logger.error("Failed to jump to code", ex)
            Messages.showErrorDialog(
                project,
                "跳转失败: ${ex.message}",
                "CodeNote Error"
            )
        }
    }
    
    companion object {
        private val logger = thisLogger()
        
        /**
         * 跳转到指定的代码笔记位置
         */
        fun jumpToCode(project: Project, codeNote: CodeNote) {
            try {
                val file = File(codeNote.filePath)
                if (!file.exists()) {
                    Messages.showWarningDialog(
                        project,
                        "文件不存在: ${codeNote.filePath}",
                        "CodeNote"
                    )
                    return
                }
                
                val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
                if (virtualFile == null) {
                    Messages.showWarningDialog(
                        project,
                        "无法找到文件: ${codeNote.filePath}",
                        "CodeNote"
                    )
                    return
                }
                
                // 创建文件描述符，指定行号（从0开始计数，所以减1）
                val descriptor = OpenFileDescriptor(
                    project, 
                    virtualFile, 
                    maxOf(0, codeNote.startLine - 1), 
                    0
                )
                
                // 打开文件并跳转到指定行
                val fileEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
                if (fileEditor != null) {
                    // 选中代码范围（如果是多行）
                    if (codeNote.startLine != codeNote.endLine) {
                        val document = fileEditor.document
                        val startOffset = document.getLineStartOffset(maxOf(0, codeNote.startLine - 1))
                        val endOffset = document.getLineEndOffset(minOf(document.lineCount - 1, codeNote.endLine - 1))
                        
                        fileEditor.selectionModel.setSelection(startOffset, endOffset)
                    }
                    
                    logger.info("Successfully jumped to code: ${codeNote.getLocationInfo()}")
                } else {
                    Messages.showWarningDialog(
                        project,
                        "无法打开编辑器",
                        "CodeNote"
                    )
                }
                
            } catch (e: Exception) {
                logger.error("Failed to jump to code location", e)
                Messages.showErrorDialog(
                    project,
                    "跳转失败: ${e.message}",
                    "CodeNote Error"
                )
            }
        }
    }
}