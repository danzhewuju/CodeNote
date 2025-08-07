package com.codenote.action

import com.codenote.model.CodeNote
import com.codenote.service.CodeNoteService
import com.codenote.service.CodeAnalysisService
import com.codenote.ui.AddCodeNoteDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile
import java.time.LocalDateTime

/**
 * 添加代码笔记的Action（显示对话框供用户编辑）
 */
class AddDetailedCodeNoteAction : AnAction() {
    
    private val logger = thisLogger()
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        
        try {
            if (editor == null) {
                Messages.showWarningDialog(
                    project,
                    "请在代码编辑器中使用此功能",
                    "CodeNote"
                )
                return
            }
            
            val selectedText = getSelectedText(editor)
            if (selectedText.isNullOrBlank()) {
                Messages.showWarningDialog(
                    project,
                    "请先选择要记录的代码片段",
                    "CodeNote"
                )
                return
            }
            
            // 获取精确的行数信息
            val selectionModel = editor.selectionModel
            val document = editor.document
            val startLine = document.getLineNumber(selectionModel.selectionStart) + 1
            val endLine = document.getLineNumber(selectionModel.selectionEnd) + 1
            
            // 分析代码结构
            val codeAnalysisService = CodeAnalysisService.getInstance()
            val structureInfo = codeAnalysisService.analyzeSelectedCode(project, editor, psiFile)
            
            // 生成默认标题
            val fileName = psiFile?.name ?: "未知文件"
            val defaultTitle = "代码片段 - $fileName"
            
            val codeNote = CodeNote(
                id = java.util.UUID.randomUUID().toString(),
                title = defaultTitle,
                content = selectedText,
                filePath = psiFile?.virtualFile?.path ?: "",
                startLine = startLine,
                endLine = endLine,
                projectPath = project.basePath ?: "",
                note = "",
                tags = emptyList(),
                className = structureInfo.className,
                methodName = structureInfo.methodName,
                methodSignature = structureInfo.methodSignature,
                createdTime = LocalDateTime.now(),
                updatedTime = LocalDateTime.now()
            )
            
            // 显示添加笔记对话框
            val dialog = AddCodeNoteDialog(project, codeNote)
            if (dialog.showAndGet()) {
                val updatedNote = dialog.getCodeNote()
                val noteId = CodeNoteService.getInstance().addCodeNote(updatedNote)
                
                Messages.showInfoMessage(
                    project,
                    "✅ 代码笔记已添加！\n📁 ${updatedNote.getRelativePath()}\n📍 第${updatedNote.startLine}-${updatedNote.endLine}行",
                    "CodeNote"
                )
                
                logger.info("Detailed code note added successfully: $noteId")
            }
            
        } catch (e: Exception) {
            logger.error("Failed to execute AddDetailedCodeNoteAction", e)
            Messages.showErrorDialog(
                project,
                "执行失败: ${e.message}",
                "CodeNote Error"
            )
        }
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = true
        e.presentation.isEnabled = true
        e.presentation.text = "添加代码笔记"
        e.presentation.description = "添加代码笔记并设置标题、备注、标签"
    }
    
    /**
     * 获取选中的文本
     */
    private fun getSelectedText(editor: Editor): String? {
        val selectionModel = editor.selectionModel
        return if (selectionModel.hasSelection()) {
            selectionModel.selectedText
        } else {
            null
        }
    }
}