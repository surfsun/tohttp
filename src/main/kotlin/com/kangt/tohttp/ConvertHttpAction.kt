package com.kangt.tohttp

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import java.util.regex.Pattern
import kotlin.text.contains
import kotlin.text.indexOf
import kotlin.text.isEmpty
import kotlin.text.substring
import kotlin.text.trim

class ConvertHttpAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor?.selectionModel?.hasSelection() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return

        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            val selectedText = selectionModel.selectedText ?: ""

            //转成http插件代码
            val httpContent = convertCurlToHttpRequest(selectedText)

            //替换原内容
            replace(selectionModel, project, editor, httpContent)
        }
    }

    /**
     * 将curl改写成http插件代码
     */
    private fun convertCurlToHttpRequest(curl: String): String {
        // 1. 判断请求方法
        val isPost = curl.contains("--data") || curl.contains("--data-raw") || curl.contains("-d")
        val method = if (isPost) "POST" else "GET"


        // 2. 解析 URL
        val urlPattern = Pattern.compile("curl\\s+['\"]([^'\"]+)['\"]")
        val urlMatcher = urlPattern.matcher(curl)
        val url = if (urlMatcher.find()) urlMatcher.group(1) else ""


        // 3. 解析 Header
        val headerPattern = Pattern.compile("-H\\s+['\"]([^'\"]+)['\"]")
        val headerMatcher = headerPattern.matcher(curl)
        val headers: MutableList<String?> = kotlin.collections.ArrayList<String?>()
        while (headerMatcher.find()) {
            val header = headerMatcher.group(1)
            // IDEA HTTP 格式要求 key: value
            val idx = header.indexOf(":")
            if (idx > 0 && idx < header.length - 1) {
                val key = header.substring(0, idx).trim { it <= ' ' }
//                if ("Authorization" == key || "Content-Type" == key) {
                    val value = header.substring(idx + 1).trim { it <= ' ' }
                    headers.add(key + ": " + value)
//                }
            }
        }


        // 4. 解析 Body
        val dataPattern = Pattern.compile("(--data-raw|--data|-d)\\s+(?:'([^']*)'|\"([^\"]*)\"|([^\\s]+))")

        val dataMatcher = dataPattern.matcher(curl)
        var body = ""
        if (dataMatcher.find()) {
            body = dataMatcher.group(2) ?: dataMatcher.group(3) ?: dataMatcher.group(4) ?: ""
            // 尝试美化 JSON
            body = JsonUtil.prettyJson(body) ?: ""
        }


        // 5. 组装为 IDEA HTTP 请求格式
        val sb = kotlin.text.StringBuilder()
        sb.append("###").append("\n")
        sb.append(method).append(" ").append(url).append("\n")
        for (header in headers) {
            sb.append(header).append("\n")
        }
        if (!body.isEmpty()) {
            sb.append("\n").append(body).append("\n")
        }
        return sb.toString()
    }


    /**
     * 替换选中的内容
     */
    private fun replace(
        selectionModel: SelectionModel,
        project: Project,
        editor: Editor,
        httpContent: String
    ) {
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.replaceString(start, end, httpContent)
        }
    }
}

