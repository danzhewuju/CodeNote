package com.codenote.ui

import com.codenote.model.CodeNote
import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import java.awt.Font
import javax.swing.JList

/**
 * 代码笔记列表单元格渲染器
 */
class CodeNoteListCellRenderer : ColoredListCellRenderer<CodeNote>() {
    
    override fun customizeCellRenderer(
        list: JList<out CodeNote>,
        value: CodeNote?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        if (value == null) return
        
        // 设置边距（更紧凑）
        border = JBUI.Borders.empty(6, 10, 6, 10)
        
        // 设置图标
        icon = getFileIcon(value.filePath)
        
        // 标题（加粗）
        val title = if (value.title.isNotBlank()) value.title else "未命名笔记"
        val titleAttributes = if (selected) {
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
        } else {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.foreground())
        }
        append(title, titleAttributes)
        
        // 文件路径和位置信息在同一行（优化显示）
        appendTextPadding(JBUI.scale(4), 0)
        val displayPath = getOptimizedPath(value.getRelativePath())
        val fileName = displayPath.substringAfterLast("/")
        val pathWithoutFile = displayPath.substringBeforeLast("/", "")
        
        // 显示文件名（稍微突出）
        append("📄 $fileName", SimpleTextAttributes(
            SimpleTextAttributes.STYLE_PLAIN, 
            JBColor(0x2196F3, 0x64B5F6) // 蓝色文件名
        ))
        
        // 显示路径（更灰一些）
        if (pathWithoutFile.isNotEmpty()) {
            append(" 📁 $pathWithoutFile", SimpleTextAttributes(
                SimpleTextAttributes.STYLE_ITALIC, 
                JBColor.GRAY.darker()
            ))
        }
        
        // 行号信息（在同一行右侧）
        append(" | 📍 ${value.startLine}-${value.endLine}行", 
               SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, JBColor.GRAY))
        
        // 代码结构信息（类和方法）- 更紧凑的显示
        val structureInfo = getCompactStructureInfo(value)
        if (structureInfo.isNotBlank()) {
            appendTextPadding(JBUI.scale(4), 0)
            append("🏛️ $structureInfo", SimpleTextAttributes(
                SimpleTextAttributes.STYLE_ITALIC, 
                JBColor(0x4CAF50, 0x81C784) // 绿色主题
            ))
        }
        
        // 标签和笔记预览在同一行（紧凑显示）
        val hasTagsOrNote = value.tags.isNotEmpty() || value.note.isNotBlank()
        if (hasTagsOrNote) {
            appendTextPadding(JBUI.scale(4), 0)
            
            // 标签显示（更紧凑）
            if (value.tags.isNotEmpty()) {
                val tagsText = if (value.tags.size <= 2) {
                    value.tags.joinToString(" ") { "🏷️$it" }
                } else {
                    "${value.tags.take(2).joinToString(" ") { "🏷️$it" }} +${value.tags.size - 2}"
                }
                append(tagsText, SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_ITALIC, 
                    JBColor(0xFF9800, 0xFFB74D) // 橙色标签
                ))
            }
            
            // 笔记预览（如果有的话，在标签后面）
            if (value.note.isNotBlank()) {
                if (value.tags.isNotEmpty()) append(" | ")
                val notePreview = if (value.note.length > 40) {
                    value.note.substring(0, 40).replace("\n", " ").trim() + "..."
                } else {
                    value.note.replace("\n", " ").trim()
                }
                append("💭 $notePreview", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
            }
        }
        
        // 设置工具提示
        toolTipText = buildTooltip(value)
    }
    
    /**
     * 获取紧凑的代码结构信息
     */
    private fun getCompactStructureInfo(codeNote: CodeNote): String {
        val parts = mutableListOf<String>()
        
        codeNote.className?.let { className ->
            // 只显示类名，不显示包路径
            val simpleClassName = className.substringAfterLast(".")
            parts.add(simpleClassName)
        }
        
        codeNote.methodName?.let { methodName ->
            // 简化方法签名显示
            val simpleMethod = if (methodName.contains("(")) {
                methodName.substringBefore("(") + "()"
            } else {
                methodName
            }
            parts.add("→ $simpleMethod")
        }
        
        return parts.joinToString(" ")
    }
    
    /**
     * 优化路径显示，避免过长的路径
     */
    private fun getOptimizedPath(fullPath: String): String {
        val maxLength = 60
        if (fullPath.length <= maxLength) {
            return fullPath
        }
        
        // 尝试智能缩短路径
        val parts = fullPath.split("/")
        if (parts.size <= 2) {
            return if (fullPath.length > maxLength) {
                "...${fullPath.takeLast(maxLength - 3)}"
            } else {
                fullPath
            }
        }
        
        // 保留开头和结尾部分，中间用...替代
        val fileName = parts.last()
        val firstPart = parts.first()
        
        val remainingLength = maxLength - fileName.length - firstPart.length - 6 // 6 for ".../" 
        
        return if (remainingLength > 0) {
            "$firstPart/.../$fileName"
        } else {
            ".../$fileName"
        }
    }
    
    private fun getFileIcon(filePath: String): javax.swing.Icon {
        val extension = filePath.substringAfterLast(".", "").lowercase()
        return when (extension) {
            "java" -> AllIcons.FileTypes.Java
            "kt", "kts" -> AllIcons.FileTypes.Unknown // Kotlin 图标
            "py" -> AllIcons.FileTypes.Unknown
            "js", "ts" -> AllIcons.FileTypes.JavaScript
            "html" -> AllIcons.FileTypes.Html
            "css" -> AllIcons.FileTypes.Css
            "xml" -> AllIcons.FileTypes.Xml
            "json" -> AllIcons.FileTypes.Json
            "md" -> AllIcons.FileTypes.Unknown
            "txt" -> AllIcons.FileTypes.Text
            "sql" -> AllIcons.FileTypes.Unknown
            else -> AllIcons.FileTypes.Any_type
        }
    }
    
    private fun buildTooltip(codeNote: CodeNote): String {
        return buildString {
            append("<html><body style='width: 300px; padding: 8px;'>")
            append("<h3 style='margin: 0 0 8px 0; color: #333;'>📝 ${codeNote.getDisplayTitle()}</h3>")
            
            append("<table cellspacing='4' cellpadding='0'>")
            append("<tr><td><b>📁 文件:</b></td><td>${codeNote.getRelativePath()}</td></tr>")
            append("<tr><td><b>📍 位置:</b></td><td>第 ${codeNote.startLine}-${codeNote.endLine} 行</td></tr>")
            
            // 显示类和方法信息
            codeNote.className?.let { className ->
                append("<tr><td><b>🏛️ 类:</b></td><td>$className</td></tr>")
            }
            
            codeNote.methodName?.let { methodName ->
                val methodDisplay = codeNote.methodSignature ?: methodName
                append("<tr><td><b>🔧 方法:</b></td><td>$methodDisplay</td></tr>")
            }
            
            if (codeNote.tags.isNotEmpty()) {
                append("<tr><td><b>🏷️ 标签:</b></td><td>")
                codeNote.tags.forEach { tag ->
                    append("<span style='background-color: #e3f2fd; padding: 2px 6px; margin: 1px; border-radius: 3px; font-size: 11px;'>$tag</span> ")
                }
                append("</td></tr>")
            }
            
            append("<tr><td><b>📅 创建:</b></td><td>${codeNote.createdTime}</td></tr>")
            
            if (codeNote.note.isNotBlank()) {
                append("<tr><td colspan='2'><hr style='margin: 8px 0;'></td></tr>")
                append("<tr><td colspan='2'><b>💭 笔记内容:</b></td></tr>")
                append("<tr><td colspan='2' style='padding-top: 4px;'>")
                
                val noteContent = if (codeNote.note.length > 200) {
                    codeNote.note.substring(0, 200) + "..."
                } else {
                    codeNote.note
                }
                append("<div style='background-color: #f5f5f5; padding: 8px; border-radius: 4px; font-family: monospace; font-size: 11px; line-height: 1.4;'>")
                append(noteContent.replace("\n", "<br>").replace(" ", "&nbsp;"))
                append("</div>")
                append("</td></tr>")
            }
            
            append("</table>")
            append("<div style='margin-top: 8px; font-size: 10px; color: #666; font-style: italic;'>双击跳转到代码 | 右键查看更多选项</div>")
            append("</body></html>")
        }
    }
}