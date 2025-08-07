package com.codenote.service

import com.codenote.model.CodeNote
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 导入导出服务
 */
@Service(Service.Level.APP)
class ImportExportService {
    
    companion object {
        fun getInstance(): ImportExportService = ApplicationManager.getApplication().getService(ImportExportService::class.java)
        
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }
    
    private val codeNoteService = CodeNoteService.getInstance()
    
    /**
     * 导出笔记到JSON文件
     */
    fun exportNotes(project: Project, exportType: ExportType = ExportType.ALL_NOTES): Boolean {
        return try {
            val notes = when (exportType) {
                ExportType.ALL_NOTES -> codeNoteService.getAllCodeNotes()
                ExportType.CURRENT_PROJECT -> {
                    if (project.basePath != null) {
                        codeNoteService.getCodeNotesByProject(project.basePath!!)
                    } else {
                        emptyList()
                    }
                }
            }
            
            if (notes.isEmpty()) {
                Messages.showInfoMessage(project, "没有找到可导出的笔记", "导出笔记")
                return false
            }
            
            // 选择保存位置
            val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            descriptor.title = "选择导出目录"
            descriptor.description = "选择保存导出文件的目录"
            
            val selectedDir = FileChooser.chooseFile(descriptor, project, null)
            if (selectedDir == null) {
                return false
            }
            
            // 生成文件名
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val projectName = if (exportType == ExportType.CURRENT_PROJECT && project.name.isNotEmpty()) {
                "_${project.name}"
            } else {
                ""
            }
            val fileName = "codenotes${projectName}_$timestamp.json"
            
            // 创建导出数据
            val exportData = ExportData(
                version = "1.0",
                exportTime = LocalDateTime.now().toString(),
                exportType = exportType.name,
                projectName = if (exportType == ExportType.CURRENT_PROJECT) project.name else null,
                projectPath = if (exportType == ExportType.CURRENT_PROJECT) project.basePath else null,
                totalCount = notes.size,
                notes = notes
            )
            
            // 写入文件
            val exportFile = File(selectedDir.path, fileName)
            exportFile.writeText(json.encodeToString(exportData))
            
            Messages.showInfoMessage(
                project, 
                "成功导出 ${notes.size} 条笔记到:\n${exportFile.absolutePath}", 
                "导出完成"
            )
            
            true
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "导出失败: ${e.message}", "导出错误")
            false
        }
    }
    
    /**
     * 从JSON文件导入笔记
     */
    fun importNotes(project: Project, importMode: ImportMode = ImportMode.MERGE): Boolean {
        return try {
            // 选择导入文件
            val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            descriptor.title = "选择导入文件"
            descriptor.description = "选择要导入的JSON文件"
            descriptor.withFileFilter { file ->
                file.extension?.lowercase() == "json"
            }
            
            val selectedFile = FileChooser.chooseFile(descriptor, project, null)
            if (selectedFile == null) {
                return false
            }
            
            // 读取并解析文件
            val fileContent = File(selectedFile.path).readText()
            val importData = try {
                json.decodeFromString<ExportData>(fileContent)
            } catch (e: Exception) {
                // 尝试直接解析为笔记列表（兼容旧格式）
                json.decodeFromString<List<CodeNote>>(fileContent).let { notes ->
                    ExportData(
                        version = "legacy",
                        exportTime = "unknown",
                        exportType = "UNKNOWN",
                        projectName = null,
                        projectPath = null,
                        totalCount = notes.size,
                        notes = notes
                    )
                }
            }
            
            if (importData.notes.isEmpty()) {
                Messages.showInfoMessage(project, "导入文件中没有找到笔记数据", "导入笔记")
                return false
            }
            
            // 显示导入确认对话框
            val message = buildString {
                append("准备导入 ${importData.totalCount} 条笔记\n\n")
                append("文件信息:\n")
                append("• 版本: ${importData.version}\n")
                append("• 导出时间: ${importData.exportTime}\n")
                if (importData.projectName != null) {
                    append("• 原项目: ${importData.projectName}\n")
                }
                append("\n导入模式: ${importMode.description}")
            }
            
            val result = Messages.showYesNoDialog(
                project,
                message,
                "确认导入",
                "导入",
                "取消",
                Messages.getQuestionIcon()
            )
            
            if (result != Messages.YES) {
                return false
            }
            
            // 执行导入
            var importedCount = 0
            var duplicateCount = 0
            var errorCount = 0
            
            when (importMode) {
                ImportMode.MERGE -> {
                    // 合并模式：跳过已存在的笔记
                    val existingNotes = codeNoteService.getAllCodeNotes()
                    val existingIds = existingNotes.map { it.id }.toSet()
                    
                    importData.notes.forEach { note ->
                        try {
                            if (existingIds.contains(note.id)) {
                                duplicateCount++
                            } else {
                                codeNoteService.addCodeNote(note)
                                importedCount++
                            }
                        } catch (e: Exception) {
                            errorCount++
                        }
                    }
                }
                
                ImportMode.REPLACE -> {
                    // 替换模式：清空现有笔记后导入
                    val existingNotes = codeNoteService.getAllCodeNotes()
                    existingNotes.forEach { note ->
                        codeNoteService.deleteCodeNote(note.id)
                    }
                    
                    importData.notes.forEach { note ->
                        try {
                            codeNoteService.addCodeNote(note)
                            importedCount++
                        } catch (e: Exception) {
                            errorCount++
                        }
                    }
                }
                
                ImportMode.ADD_ALL -> {
                    // 全部添加模式：为重复笔记生成新ID
                    importData.notes.forEach { note ->
                        try {
                            val newNote = note.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                createdTime = LocalDateTime.now()
                            )
                            codeNoteService.addCodeNote(newNote)
                            importedCount++
                        } catch (e: Exception) {
                            errorCount++
                        }
                    }
                }
            }
            
            // 显示导入结果
            val resultMessage = buildString {
                append("导入完成!\n\n")
                append("• 成功导入: $importedCount 条\n")
                if (duplicateCount > 0) {
                    append("• 跳过重复: $duplicateCount 条\n")
                }
                if (errorCount > 0) {
                    append("• 导入失败: $errorCount 条\n")
                }
            }
            
            Messages.showInfoMessage(project, resultMessage, "导入结果")
            
            true
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "导入失败: ${e.message}", "导入错误")
            false
        }
    }
    
    /**
     * 导出类型
     */
    enum class ExportType(val description: String) {
        ALL_NOTES("所有笔记"),
        CURRENT_PROJECT("当前项目笔记")
    }
    
    /**
     * 导入模式
     */
    enum class ImportMode(val description: String) {
        MERGE("合并模式 - 跳过重复笔记"),
        REPLACE("替换模式 - 清空现有笔记"),
        ADD_ALL("全部添加 - 重复笔记生成新ID")
    }
    
    /**
     * 导出数据结构
     */
    @kotlinx.serialization.Serializable
    data class ExportData(
        val version: String,
        val exportTime: String,
        val exportType: String,
        val projectName: String?,
        val projectPath: String?,
        val totalCount: Int,
        val notes: List<CodeNote>
    )
}