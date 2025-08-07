package com.codenote.service

import com.codenote.model.CodeNote
import com.codenote.settings.CodeNoteSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class SerializableCodeNote(
    val id: String,
    val title: String,
    val content: String,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val projectPath: String,
    val note: String = "",
    val tags: List<String> = emptyList(),
    val className: String? = null,
    val methodName: String? = null,
    val methodSignature: String? = null,
    val createdTime: String,
    val updatedTime: String
)

/**
 * 代码笔记服务
 */
@Service(Service.Level.APP)
class CodeNoteService {
    
    private val logger = thisLogger()
    private val codeNotes = ConcurrentHashMap<String, CodeNote>()
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    companion object {
        fun getInstance(): CodeNoteService {
            return ApplicationManager.getApplication().getService(CodeNoteService::class.java)
        }
    }
    
    init {
        loadNotesFromStorage()
    }
    
    /**
     * 添加代码笔记
     */
    fun addCodeNote(codeNote: CodeNote): String {
        val id = codeNote.id.ifEmpty { UUID.randomUUID().toString() }
        val noteWithId = codeNote.copy(id = id)
        codeNotes[id] = noteWithId
        saveNotesToStorage()
        logger.info("Added code note: $id")
        return id
    }
    
    /**
     * 更新代码笔记
     */
    fun updateCodeNote(id: String, updatedNote: CodeNote) {
        if (codeNotes.containsKey(id)) {
            codeNotes[id] = updatedNote.copy(id = id, updatedTime = LocalDateTime.now())
            saveNotesToStorage()
            logger.info("Updated code note: $id")
        }
    }
    
    /**
     * 删除代码笔记
     */
    fun deleteCodeNote(id: String) {
        codeNotes.remove(id)
        saveNotesToStorage()
        logger.info("Deleted code note: $id")
    }
    
    /**
     * 获取所有代码笔记
     */
    fun getAllCodeNotes(): List<CodeNote> {
        return codeNotes.values.sortedByDescending { it.createdTime }
    }
    
    /**
     * 根据项目路径获取代码笔记
     */
    fun getCodeNotesByProject(projectPath: String): List<CodeNote> {
        return codeNotes.values
            .filter { it.projectPath == projectPath }
            .sortedByDescending { it.createdTime }
    }
    
    /**
     * 根据ID获取代码笔记
     */
    fun getCodeNote(id: String): CodeNote? {
        return codeNotes[id]
    }
    

    
    /**
     * 搜索代码笔记
     */
    fun searchCodeNotes(query: String): List<CodeNote> {
        if (query.isBlank()) return getAllCodeNotes()
        
        val lowerQuery = query.lowercase()
        return codeNotes.values.filter { note ->
            note.title.lowercase().contains(lowerQuery) ||
            note.content.lowercase().contains(lowerQuery) ||
            note.note.lowercase().contains(lowerQuery) ||
            note.tags.any { it.lowercase().contains(lowerQuery) } ||
            note.filePath.lowercase().contains(lowerQuery)
        }.sortedByDescending { it.createdTime }
    }
    
    /**
     * 从存储加载笔记（从所有项目目录）
     */
    private fun loadNotesFromStorage() {
        try {
            codeNotes.clear()
            val settings = CodeNoteSettings.getInstance()
            val storagePath = settings.storagePath.ifBlank { 
                System.getProperty("user.home") + File.separator + ".codenote"
            }
            val storageDir = File(storagePath)
            
            if (!storageDir.exists()) {
                return
            }
            
            // 加载根目录的笔记（向后兼容）
            val rootStorageFile = File(storageDir, "codenotes.json")
            loadNotesFromFile(rootStorageFile)
            
            // 加载所有项目目录的笔记
            storageDir.listFiles()?.forEach { projectDir ->
                if (projectDir.isDirectory) {
                    val projectStorageFile = File(projectDir, "codenotes.json")
                    loadNotesFromFile(projectStorageFile)
                }
            }
            
            logger.info("Loaded ${codeNotes.size} code notes from storage")
        } catch (e: Exception) {
            logger.warn("Failed to load code notes from storage", e)
        }
    }
    
    /**
     * 从指定文件加载笔记
     */
    private fun loadNotesFromFile(storageFile: File) {
        try {
            if (storageFile.exists()) {
                val content = storageFile.readText()
                if (content.isNotBlank()) {
                    val serializableNotes = json.decodeFromString<List<SerializableCodeNote>>(content)
                    
                    for (serializable in serializableNotes) {
                        val codeNote = CodeNote(
                            id = serializable.id,
                            title = serializable.title,
                            content = serializable.content,
                            filePath = serializable.filePath,
                            startLine = serializable.startLine,
                            endLine = serializable.endLine,
                            projectPath = serializable.projectPath,
                            note = serializable.note,
                            tags = serializable.tags,
                            className = serializable.className,
                            methodName = serializable.methodName,
                            methodSignature = serializable.methodSignature,
                            createdTime = LocalDateTime.parse(serializable.createdTime),
                            updatedTime = LocalDateTime.parse(serializable.updatedTime)
                        )
                        codeNotes[codeNote.id] = codeNote
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to load code notes from file: ${storageFile.path}", e)
        }
    }
    
    /**
     * 保存笔记到存储（按项目分组）
     */
    private fun saveNotesToStorage() {
        try {
            // 按项目分组保存笔记
            val notesByProject = codeNotes.values.groupBy { it.projectPath }
            
            for ((projectPath, notes) in notesByProject) {
                val storageFile = getStorageFile(projectPath)
                storageFile.parentFile?.mkdirs()
                
                val serializableNotes = notes.map { note ->
                    SerializableCodeNote(
                        id = note.id,
                        title = note.title,
                        content = note.content,
                        filePath = note.filePath,
                        startLine = note.startLine,
                        endLine = note.endLine,
                        projectPath = note.projectPath,
                        note = note.note,
                        tags = note.tags,
                        className = note.className,
                        methodName = note.methodName,
                        methodSignature = note.methodSignature,
                        createdTime = note.createdTime.toString(),
                        updatedTime = note.updatedTime.toString()
                    )
                }
                
                val content = json.encodeToString(serializableNotes)
                storageFile.writeText(content)
                
                val projectName = if (projectPath.isNotBlank()) File(projectPath).name else "default"
                logger.info("Saved ${notes.size} code notes for project: $projectName")
            }
        } catch (e: Exception) {
            logger.error("Failed to save code notes to storage", e)
        }
    }
    
    /**
     * 获取存储文件（按项目分组）
     */
    private fun getStorageFile(projectPath: String = ""): File {
        val settings = CodeNoteSettings.getInstance()
        val storagePath = settings.storagePath.ifBlank { 
            System.getProperty("user.home") + File.separator + ".codenote"
        }
        val storageDir = File(storagePath)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        // 如果有项目路径，为项目创建单独的存储文件
        return if (projectPath.isNotBlank()) {
            val projectName = File(projectPath).name
            val projectDir = File(storageDir, projectName)
            if (!projectDir.exists()) {
                projectDir.mkdirs()
            }
            File(projectDir, "codenotes.json")
        } else {
            File(storagePath, "codenotes.json")
        }
    }
    
    /**
     * 获取所有项目的笔记
     */
    fun getAllProjects(): List<String> {
        return codeNotes.values
            .map { it.projectPath }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    
    /**
     * 跳转到代码位置
     */
    fun navigateToCode(project: Project, codeNote: CodeNote) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val virtualFile = VirtualFileManager.getInstance()
                    .findFileByUrl("file://${codeNote.filePath}")
                
                if (virtualFile != null && virtualFile.exists()) {
                    val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                    val editor = fileEditorManager.openFile(virtualFile, true).firstOrNull()
                    
                    if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                        val textEditor = editor.editor
                        val document = textEditor.document
                        val startOffset = document.getLineStartOffset(maxOf(0, codeNote.startLine - 1))
                        val endOffset = if (codeNote.endLine <= document.lineCount) {
                            document.getLineEndOffset(codeNote.endLine - 1)
                        } else {
                            document.textLength
                        }
                        
                        textEditor.caretModel.moveToOffset(startOffset)
                        textEditor.selectionModel.setSelection(startOffset, endOffset)
                        textEditor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
                    }
                } else {
                    logger.warn("File not found: ${codeNote.filePath}")
                }
            } catch (e: Exception) {
                logger.error("Failed to navigate to code", e)
            }
        }
    }
}