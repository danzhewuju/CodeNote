package com.codenote.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * LocalDateTime序列化器
 */
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }
    
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

/**
 * 代码笔记数据模型
 */
@Serializable
data class CodeNote(
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
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdTime: LocalDateTime = LocalDateTime.now(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedTime: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 获取代码片段的位置信息
     */
    fun getLocationInfo(): String {
        return "$filePath:$startLine-$endLine"
    }
    
    /**
     * 获取相对路径
     */
    fun getRelativePath(): String {
        return if (filePath.startsWith(projectPath)) {
            filePath.substring(projectPath.length).removePrefix("/")
        } else {
            filePath
        }
    }
    
    /**
     * 获取显示用的标题
     */
    fun getDisplayTitle(): String {
        return if (title.isNotBlank()) title else "代码片段 - ${getRelativePath()}"
    }
    
    /**
     * 获取代码结构信息
     */
    fun getCodeStructureInfo(): String {
        val parts = mutableListOf<String>()
        
        className?.let { parts.add("🏛️ $it") }
        methodName?.let { 
            if (methodSignature != null) {
                parts.add("🔧 $methodSignature")
            } else {
                parts.add("🔧 $it")
            }
        }
        
        return if (parts.isNotEmpty()) parts.joinToString(" ") else ""
    }
}