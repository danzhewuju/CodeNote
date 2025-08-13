package com.codenote.action

import com.codenote.model.CodeNote
import com.codenote.util.PathUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.JarFileSystem
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
                val virtualFile = findVirtualFile(codeNote.filePath)
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
        
        /**
         * 根据路径查找虚拟文件，支持普通文件和JAR内部文件
         */
        private fun findVirtualFile(filePath: String): com.intellij.openapi.vfs.VirtualFile? {
            return try {
                if (PathUtils.isJarPath(filePath)) {
                    // 处理JAR内部文件路径
                    val jarPathInfo = PathUtils.parseJarPath(filePath)
                    if (jarPathInfo != null) {
                        // 检查JAR文件是否存在
                        if (!PathUtils.jarFileExists(jarPathInfo.jarFilePath)) {
                            logger.warn("JAR file not found: ${jarPathInfo.jarFilePath}")
                            return null
                        }
                        
                        // 尝试找到源文件（.java）而不是类文件（.class）
                        val sourcePath = PathUtils.inferSourcePath(jarPathInfo.internalPath)
                        
                        // 首先尝试获取JAR文件的虚拟文件
                        val jarVirtualFile = LocalFileSystem.getInstance().findFileByPath(jarPathInfo.jarFilePath)
                        if (jarVirtualFile != null) {
                            val jarFileSystem = JarFileSystem.getInstance()
                            val jarRoot = jarFileSystem.getJarRootForLocalFile(jarVirtualFile)
                            if (jarRoot != null) {
                                // 先尝试找源文件
                                var internalFile = jarRoot.findFileByRelativePath(sourcePath)
                                if (internalFile == null) {
                                    // 如果没有源文件，尝试找类文件
                                    internalFile = jarRoot.findFileByRelativePath(jarPathInfo.internalPath)
                                }
                                return internalFile
                            }
                        }
                        
                        // 如果上面的方法失败，尝试使用URL方式
                        val jarUrl = PathUtils.buildJarFileUrl(jarPathInfo.jarFilePath, sourcePath)
                        var virtualFile = VirtualFileManager.getInstance().findFileByUrl(jarUrl)
                        if (virtualFile == null) {
                            // 尝试类文件
                            val classUrl = PathUtils.buildJarFileUrl(jarPathInfo.jarFilePath, jarPathInfo.internalPath)
                            virtualFile = VirtualFileManager.getInstance().findFileByUrl(classUrl)
                        }
                        return virtualFile
                    }
                } else {
                    // 处理普通文件路径
                    val file = File(filePath)
                    if (!file.exists()) {
                        logger.warn("File not found: $filePath")
                        return null
                    }
                    return LocalFileSystem.getInstance().findFileByIoFile(file)
                }
                null
            } catch (e: Exception) {
                logger.error("Error finding virtual file for path: $filePath", e)
                null
            }
        }
    }
}