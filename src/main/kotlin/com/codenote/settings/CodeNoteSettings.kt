package com.codenote.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File

/**
 * CodeNote插件设置
 */
@State(
    name = "CodeNoteSettings",
    storages = [Storage("codenote-settings.xml")]
)
@Service(Service.Level.APP)
class CodeNoteSettings : PersistentStateComponent<CodeNoteSettings> {
    
    var storagePath: String = ""
    var maxNotesPerProject: Int = 1000
    var autoSave: Boolean = true
    var showLineNumbers: Boolean = true
    var enableSyntaxHighlight: Boolean = true
    
    companion object {
        fun getInstance(): CodeNoteSettings {
            return ApplicationManager.getApplication().getService(CodeNoteSettings::class.java)
        }
    }
    
    override fun getState(): CodeNoteSettings {
        return this
    }
    
    override fun loadState(state: CodeNoteSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
    
    /**
     * 获取默认存储路径
     */
    fun getDefaultStoragePath(): String {
        return System.getProperty("user.home") + File.separator + ".codenote"
    }
    
    /**
     * 获取实际使用的存储路径
     */
    fun getEffectiveStoragePath(): String {
        return if (storagePath.isBlank()) getDefaultStoragePath() else storagePath
    }
}