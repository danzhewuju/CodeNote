package com.codenote.util

import java.io.File

/**
 * 路径处理工具类
 */
object PathUtils {
    
    /**
     * JAR文件路径信息
     */
    data class JarPathInfo(
        val jarFilePath: String,
        val internalPath: String,
        val isJarPath: Boolean = true
    )
    
    /**
     * 检查路径是否为JAR内部文件路径
     * JAR路径格式: /path/to/file.jar!/internal/path/to/file.class
     */
    fun isJarPath(path: String): Boolean {
        return path.contains(".jar!") || path.contains(".JAR!")
    }
    
    /**
     * 解析JAR路径，分离JAR文件路径和内部文件路径
     */
    fun parseJarPath(path: String): JarPathInfo? {
        if (!isJarPath(path)) {
            return null
        }
        
        val jarSeparatorIndex = path.indexOf(".jar!")
        if (jarSeparatorIndex == -1) {
            val jarSeparatorIndexUpper = path.indexOf(".JAR!")
            if (jarSeparatorIndexUpper == -1) {
                return null
            }
            val jarFilePath = path.substring(0, jarSeparatorIndexUpper + 4) // +4 for ".JAR"
            val internalPath = path.substring(jarSeparatorIndexUpper + 5) // +5 for ".JAR!"
            return JarPathInfo(jarFilePath, internalPath)
        }
        
        val jarFilePath = path.substring(0, jarSeparatorIndex + 4) // +4 for ".jar"
        val internalPath = path.substring(jarSeparatorIndex + 5) // +5 for ".jar!"
        
        return JarPathInfo(jarFilePath, internalPath)
    }
    
    /**
     * 检查JAR文件是否存在
     */
    fun jarFileExists(jarPath: String): Boolean {
        return try {
            val jarFile = File(jarPath)
            jarFile.exists() && jarFile.isFile && (jarPath.endsWith(".jar") || jarPath.endsWith(".JAR"))
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 构建JAR内部文件的URL
     */
    fun buildJarFileUrl(jarFilePath: String, internalPath: String): String {
        return "jar:file://$jarFilePath!/$internalPath"
    }
    
    /**
     * 从类文件路径推断可能的源文件路径
     */
    fun inferSourcePath(classPath: String): String {
        return if (classPath.endsWith(".class")) {
            classPath.substring(0, classPath.length - 6) + ".java"
        } else {
            classPath
        }
    }
}
