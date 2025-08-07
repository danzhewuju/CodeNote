package com.codenote.startup

import com.codenote.service.ShortcutManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * 插件启动活动
 * 在项目打开时执行初始化操作
 */
class CodeNoteStartupActivity : StartupActivity {
    
    override fun runActivity(project: Project) {
        // 应用保存的快捷键设置
        ShortcutManager.getInstance().applyCustomShortcut()
    }
}