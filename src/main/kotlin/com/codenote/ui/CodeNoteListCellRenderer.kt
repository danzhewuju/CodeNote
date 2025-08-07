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
        
        // 设置边距
        border = JBUI.Borders.empty(8, 12, 8, 12)
        
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
        
        // 换行显示文件信息
        appendTextPadding(JBUI.scale(8), 0)
        append("📁 ${value.getRelativePath()}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        
        // 行号信息
        append(" 📍 ${value.startLine}-${value.endLine}行", 
               SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, JBColor.GRAY))
        
        // 代码结构信息（类和方法）
        val structureInfo = value.getCodeStructureInfo()
        if (structureInfo.isNotBlank()) {
            appendTextPadding(JBUI.scale(8), 0)
            append(structureInfo, SimpleTextAttributes(
                SimpleTextAttributes.STYLE_ITALIC, 
                JBColor(0x4CAF50, 0x81C784) // 绿色主题
            ))
        }
        
        // 标签显示（如果有的话）
        if (value.tags.isNotEmpty()) {
            appendTextPadding(JBUI.scale(8), 0)
            value.tags.forEachIndexed { i, tag ->
                if (i > 0) append(" ")
                append("🏷️$tag", SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_ITALIC, 
                    JBColor.BLUE
                ))
            }
        }
        
        // 笔记预览（如果有的话）
        if (value.note.isNotBlank()) {
            appendTextPadding(JBUI.scale(8), 0)
            val notePreview = if (value.note.length > 50) {
                value.note.substring(0, 50).replace("\n", " ") + "..."
            } else {
                value.note.replace("\n", " ")
            }
            append("💭 $notePreview", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
        }
        
        // 设置工具提示
        toolTipText = buildTooltip(value)
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