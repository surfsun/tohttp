package com.kangt.tohttp

import kotlin.collections.withIndex
import kotlin.text.endsWith
import kotlin.text.indices
import kotlin.text.isEmpty
import kotlin.text.repeat
import kotlin.text.startsWith
import kotlin.text.substring
import kotlin.text.trim

class JsonUtil {
    companion object {
        /**
         * 支持嵌套的 JSON 美化
         */
        fun prettyJson(json: String?): String? {
            if (json == null || json.isEmpty()) return json

            try {
                val input = json.trim()
                if (input.startsWith("{") && input.endsWith("}")) {
                    return formatObject(input, 0)
                } else if (input.startsWith("[") && input.endsWith("]")) {
                    return formatArray(input, 0)
                }
            } catch (e: Exception) {
                // 解析失败时返回原字符串
                return json
            }

            return json
        }

        private fun formatObject(obj: String, indentLevel: Int): String {
            val result = kotlin.text.StringBuilder()
            val indent = "  ".repeat(indentLevel)
            val nextIndent = "  ".repeat(indentLevel + 1)

            result.append("{\n")

            val content = obj.substring(1, obj.length - 1).trim()
            if (content.isEmpty()) {
                result.append("}")
                return result.toString()
            }

            val pairs = splitJsonPairs(content)

            for ((index, pair) in pairs.withIndex()) {
                val colonIndex = findColonIndex(pair.trim())
                if (colonIndex != -1) {
                    val key = pair.substring(0, colonIndex).trim()
                    val value = pair.substring(colonIndex + 1).trim()

                    result.append(nextIndent).append(key).append(": ")

                    when {
                        value.startsWith("{") -> result.append(formatObject(value, indentLevel + 1))
                        value.startsWith("[") -> result.append(formatArray(value, indentLevel + 1))
                        else -> result.append(value)
                    }

                    if (index < pairs.size - 1) {
                        result.append(",")
                    }
                    result.append("\n")
                }
            }

            result.append(indent).append("}")
            return result.toString()
        }

        private fun formatArray(arr: String, indentLevel: Int): String {
            val result = kotlin.text.StringBuilder()
            val indent = "  ".repeat(indentLevel)
            val nextIndent = "  ".repeat(indentLevel + 1)

            result.append("[\n")

            val content = arr.substring(1, arr.length - 1).trim()
            if (content.isEmpty()) {
                result.append("]")
                return result.toString()
            }

            val elements = splitJsonArrayElements(content)

            for ((index, element) in elements.withIndex()) {
                val trimmed = element.trim()
                result.append(nextIndent)

                when {
                    trimmed.startsWith("{") -> result.append(formatObject(trimmed, indentLevel + 1))
                    trimmed.startsWith("[") -> result.append(formatArray(trimmed, indentLevel + 1))
                    else -> result.append(trimmed)
                }

                if (index < elements.size - 1) {
                    result.append(",")
                }
                result.append("\n")
            }

            result.append(indent).append("]")
            return result.toString()
        }

        private fun splitJsonPairs(content: String): List<String> {
            val pairs = mutableListOf<String>()
            var braceCount = 0
            var bracketCount = 0
            var start = 0
            var inString = false
            var escapeNext = false

            for (i in content.indices) {
                val ch = content[i]

                if (escapeNext) {
                    escapeNext = false
                    continue
                }

                if (ch == '\\' && inString) {
                    escapeNext = true
                    continue
                }

                if (ch == '"' && !escapeNext) {
                    inString = !inString
                    continue
                }

                if (!inString) {
                    when (ch) {
                        '{' -> braceCount++
                        '}' -> braceCount--
                        '[' -> bracketCount++
                        ']' -> bracketCount--
                        ',' -> {
                            if (braceCount == 0 && bracketCount == 0) {
                                pairs.add(content.substring(start, i))
                                start = i + 1
                            }
                        }
                    }
                }
            }

            if (start < content.length) {
                pairs.add(content.substring(start))
            }

            return pairs
        }

        private fun splitJsonArrayElements(content: String): List<String> {
            val elements = mutableListOf<String>()
            var braceCount = 0
            var bracketCount = 0
            var start = 0
            var inString = false
            var escapeNext = false

            for (i in content.indices) {
                val ch = content[i]

                if (escapeNext) {
                    escapeNext = false
                    continue
                }

                if (ch == '\\' && inString) {
                    escapeNext = true
                    continue
                }

                if (ch == '"' && !escapeNext) {
                    inString = !inString
                    continue
                }

                if (!inString) {
                    when (ch) {
                        '{' -> braceCount++
                        '}' -> braceCount--
                        '[' -> bracketCount++
                        ']' -> bracketCount--
                        ',' -> {
                            if (braceCount == 0 && bracketCount == 0) {
                                elements.add(content.substring(start, i))
                                start = i + 1
                            }
                        }
                    }
                }
            }

            if (start < content.length) {
                elements.add(content.substring(start))
            }

            return elements
        }

        private fun findColonIndex(str: String): Int {
            var braceCount = 0
            var bracketCount = 0
            var inString = false
            var escapeNext = false

            for (i in str.indices) {
                val ch = str[i]

                if (escapeNext) {
                    escapeNext = false
                    continue
                }

                if (ch == '\\' && inString) {
                    escapeNext = true
                    continue
                }

                if (ch == '"' && !escapeNext) {
                    inString = !inString
                    continue
                }

                if (!inString) {
                    when (ch) {
                        '{' -> braceCount++
                        '}' -> braceCount--
                        '[' -> bracketCount++
                        ']' -> bracketCount--
                        ':' -> {
                            if (braceCount == 0 && bracketCount == 0) {
                                return i
                            }
                        }
                    }
                }
            }

            return -1
        }
    }
}