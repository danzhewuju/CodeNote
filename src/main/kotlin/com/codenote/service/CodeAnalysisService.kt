package com.codenote.service

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

/**
 * 代码分析服务
 * 用于分析代码片段的结构信息，如类、方法等
 */
class CodeAnalysisService {
    
    companion object {
        @JvmStatic
        fun getInstance(): CodeAnalysisService = CodeAnalysisService()
    }
    
    /**
     * 分析选中的代码片段，提取类和方法信息
     */
    fun analyzeSelectedCode(
        project: Project,
        editor: Editor,
        psiFile: PsiFile?
    ): CodeStructureInfo {
        if (psiFile == null) {
            return CodeStructureInfo()
        }
        
        val selectionModel = editor.selectionModel
        val startOffset = selectionModel.selectionStart
        val endOffset = selectionModel.selectionEnd
        
        // 获取选择范围内的PSI元素
        val startElement = psiFile.findElementAt(startOffset)
        val endElement = psiFile.findElementAt(endOffset - 1)
        
        if (startElement == null) {
            return CodeStructureInfo()
        }
        
        // 查找包含选择区域的最小公共父元素
        val commonParent = if (endElement != null && startElement != endElement) {
            PsiTreeUtil.findCommonParent(startElement, endElement)
        } else {
            startElement
        }
        
        return analyzeElement(commonParent)
    }
    
    /**
     * 分析PSI元素，提取结构信息
     */
    private fun analyzeElement(element: PsiElement?): CodeStructureInfo {
        if (element == null) {
            return CodeStructureInfo()
        }
        
        return try {
            var className: String? = null
            var methodName: String? = null
            var methodSignature: String? = null
            
            // 检查是否是Kotlin文件
            if (isKotlinFile(element.containingFile)) {
                // Kotlin代码分析（使用反射）
                val structureInfo = analyzeKotlinElement(element)
                className = structureInfo.className
                methodName = structureInfo.methodName
                methodSignature = structureInfo.methodSignature
            } else {
                // Java代码分析
                className = findJavaClassName(element)
                val methodInfo = findJavaMethodInfo(element)
                methodName = methodInfo.first
                methodSignature = methodInfo.second
            }
            
            CodeStructureInfo(className, methodName, methodSignature)
        } catch (e: Exception) {
            // 如果所有分析都失败，返回空信息
            CodeStructureInfo()
        }
    }
    
    /**
     * 安全地查找Java类名
     */
    @Suppress("UNCHECKED_CAST")
    private fun findJavaClassName(element: PsiElement): String? {
        return try {
            // 使用反射查找PsiClass
            val psiClassType = Class.forName("com.intellij.psi.PsiClass") as Class<out PsiElement>
            val containingClass = PsiTreeUtil.getParentOfType(element, psiClassType)
            containingClass?.let { 
                val nameMethod = it.javaClass.getMethod("getName")
                nameMethod.invoke(it) as? String
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 安全地查找Java方法信息
     */
    @Suppress("UNCHECKED_CAST")
    private fun findJavaMethodInfo(element: PsiElement): Pair<String?, String?> {
        return try {
            // 使用反射查找PsiMethod
            val psiMethodType = Class.forName("com.intellij.psi.PsiMethod") as Class<out PsiElement>
            val containingMethod = PsiTreeUtil.getParentOfType(element, psiMethodType)
            containingMethod?.let { 
                val nameMethod = it.javaClass.getMethod("getName")
                val methodName = nameMethod.invoke(it) as? String
                val signature = "$methodName(...)" // 简化签名
                Pair(methodName, signature)
            } ?: Pair(null, null)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }
    

    
    /**
     * 检查是否是Kotlin文件
     */
    private fun isKotlinFile(file: PsiFile?): Boolean {
        return try {
            file?.javaClass?.name?.contains("kotlin", ignoreCase = true) == true ||
            file?.fileType?.name?.equals("Kotlin", ignoreCase = true) == true ||
            file?.virtualFile?.extension?.equals("kt", ignoreCase = true) == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 使用反射安全地分析Kotlin元素
     */
    private fun analyzeKotlinElement(element: PsiElement): CodeStructureInfo {
        return try {
            var className: String? = null
            var methodName: String? = null
            var methodSignature: String? = null
            
            // 使用反射查找Kotlin类
            val ktClass = findKotlinClass(element)
            ktClass?.let { 
                className = getKotlinElementName(it)
            }
            
            // 使用反射查找Kotlin函数
            val ktFunction = findKotlinFunction(element)
            ktFunction?.let { 
                methodName = getKotlinElementName(it)
                methodSignature = buildKotlinFunctionSignatureReflection(it)
            }
            
            // 如果没有找到函数，查找构造函数
            if (ktFunction == null) {
                val ktConstructor = findKotlinConstructor(element)
                ktConstructor?.let {
                    methodName = "<init>"
                    methodSignature = buildKotlinConstructorSignatureReflection(it, className)
                }
            }
            
            CodeStructureInfo(className, methodName, methodSignature)
        } catch (e: Exception) {
            CodeStructureInfo()
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun findKotlinClass(element: PsiElement): PsiElement? {
        return try {
            // 使用反射查找KtClass
            val ktClassType = Class.forName("org.jetbrains.kotlin.psi.KtClass") as Class<out PsiElement>
            PsiTreeUtil.getParentOfType(element, ktClassType)
        } catch (e: Exception) {
            null
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun findKotlinFunction(element: PsiElement): PsiElement? {
        return try {
            // 使用反射查找KtNamedFunction
            val ktFunctionType = Class.forName("org.jetbrains.kotlin.psi.KtNamedFunction") as Class<out PsiElement>
            PsiTreeUtil.getParentOfType(element, ktFunctionType)
        } catch (e: Exception) {
            null
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun findKotlinConstructor(element: PsiElement): PsiElement? {
        return try {
            // 查找主构造函数
            val ktPrimaryConstructorType = Class.forName("org.jetbrains.kotlin.psi.KtPrimaryConstructor") as Class<out PsiElement>
            val primary = PsiTreeUtil.getParentOfType(element, ktPrimaryConstructorType)
            if (primary != null) return primary
            
            // 查找次构造函数
            val ktSecondaryConstructorType = Class.forName("org.jetbrains.kotlin.psi.KtSecondaryConstructor") as Class<out PsiElement>
            PsiTreeUtil.getParentOfType(element, ktSecondaryConstructorType)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getKotlinElementName(element: PsiElement): String? {
        return try {
            val nameMethod = element.javaClass.getMethod("getName")
            nameMethod.invoke(element) as? String
        } catch (e: Exception) {
            null
        }
    }
    
    private fun buildKotlinFunctionSignatureReflection(function: PsiElement): String {
        return try {
            val name = getKotlinElementName(function) ?: "unknown"
            "$name(...)" // 简化签名，避免复杂的反射
        } catch (e: Exception) {
            "unknown(...)"
        }
    }
    
    private fun buildKotlinConstructorSignatureReflection(constructor: PsiElement, className: String?): String {
        return try {
            val actualClassName = className ?: "Unknown"
            "$actualClassName(...)" // 简化签名
        } catch (e: Exception) {
            "Unknown(...)"
        }
    }
}

/**
 * 代码结构信息数据类
 */
data class CodeStructureInfo(
    val className: String? = null,
    val methodName: String? = null,
    val methodSignature: String? = null
)