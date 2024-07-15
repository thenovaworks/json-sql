package io.github.thenovaworks.json.query

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*


//fun QueryResult.getInt(columnName: String, defaultValue: Int = 0): Int {
//    return data.firstOrNull()?.get(columnName)?.toString()?.toIntOrNull() ?: defaultValue
//}
//
//fun QueryResult.getString(columnName: String, defaultValue: String = ""): String {
//    return data.firstOrNull()?.get(columnName)?.toString() ?: defaultValue
//}
//
//fun QueryResult.getDate(columnName: String, defaultValue: LocalDateTime = LocalDateTime.MIN): LocalDateTime {
//    return data.firstOrNull()?.get(columnName)?.toString()?.let {
//        LocalDateTime.parse(it)
//    } ?: defaultValue
//}
//
//fun Map<String, Any>.getInt(columnName: String, defaultValue: Int = 0): Int {
//    return this[columnName]?.toString()?.toIntOrNull() ?: defaultValue
//}
//
//fun Map<String, Any>.getString(columnName: String, defaultValue: String = ""): String {
//    return this[columnName]?.toString() ?: defaultValue
//}
//
//fun Map<String, Any>.getDate(columnName: String, defaultValue: LocalDateTime = LocalDateTime.MIN): LocalDateTime {
//    return this[columnName]?.toString()?.let {
//        toLocalDateTime(it)
//    } ?: defaultValue
//}


class JsonQueryHandler(private val schemaName: String, jsonMessage: String) {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val rootNode: JsonNode = objectMapper.readTree(jsonMessage)

    fun executeQuery(query: String, params: Map<String, Any> = emptyMap()): Either<String, JsonResultSet> {
        return try {
            val selectPattern =
                Regex("(?i)select\\s+(.+)\\s+from\\s+$schemaName(?:\\s+where\\s+(.+))?", RegexOption.DOT_MATCHES_ALL)
            val matchResult = selectPattern.matchEntire(query.trim())
                ?: return "Invalid query format".left()

            val columns = matchResult.groupValues[1].split(",").map { it.trim() }
            val whereCondition = matchResult.groupValues.getOrNull(2)?.trim()
                ?.let { substituteParams(it, params) }
                ?.also { checkForUnboundParameters(it) }
            // println("CHK executeQuery.whereCondition ----- $whereCondition")

            val tokens = whereCondition?.let { tokenizeCondition(it) }
            val records = rootNode.toRecordList()
            val filteredRecords = if (tokens.isNullOrEmpty() || isAlwaysTrueCondition(whereCondition)) {
                records
            } else {
                records.filter { evaluateTokens(it, tokens) }
            }

            val results = filteredRecords.map { record ->
                JsonResultMap(columns.associateWith { column ->
                    getValueFromJson(record, column)
                })
            }

            JsonResultSet(columns, results).right()
        } catch (e: Exception) {
            e.printStackTrace()
            e.message.orEmpty().left()
        }
    }

    private fun substituteParams(condition: String, params: Map<String, Any>): String {
        var result = condition
        params.forEach { (key, value) ->
            result = result.replace(":$key", "'$value'")
        }
        return result
    }

    private fun checkForUnboundParameters(condition: String) {
        val unboundParameterPattern = Regex(":\\w+")
        val matchResult = unboundParameterPattern.find(condition)
        if (matchResult != null) {
            throw IllegalArgumentException("Unbounded parameter error: ${matchResult.value}")
        }
    }

    private fun tokenizeCondition(condition: String): List<String> {
        // println("CHK tokenizeCondition --- $condition")
        val regex = Regex("(?i)(and|or|[^\\s]+\\s*(<=|>=|=|<|>)\\s*'[^']*'|[^\\s]+\\s*(<=|>=|=|<|>)\\s*[^\\s]+)")
        return regex.findAll(condition).map { it.value.trim() }.toList()
    }

    private fun evaluateTokens(jsonNode: JsonNode, tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return true

        var result = evaluateCondition(jsonNode, tokens[0])
        var index = 1

        while (index < tokens.size) {
            val operator = tokens[index].lowercase(Locale.getDefault())
            val nextCondition = tokens[index + 1]

            when (operator) {
                "and" -> result = result && evaluateCondition(jsonNode, nextCondition)
                "or" -> result = result || evaluateCondition(jsonNode, nextCondition)
            }
            index += 2
        }

        return result
    }


    private fun compareValues(fieldValue: String, value: String): Int {
        return if (toNumber(fieldValue) is Number) {
            fieldValue.toDouble().compareTo(value.toDoubleOrNull() ?: return -1)
        } else {
            fieldValue.compareTo(value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toNumber(stringVal: String): Comparable<Number> {
        return try {
            stringVal.toIntOrNull()?.let { it as Comparable<Number> }
                ?: stringVal.toFloat() as Comparable<Number>
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid number format: $stringVal")
        }
    }

    private fun evaluateCondition(jsonNode: JsonNode, condition: String): Boolean {
        // println("CHK condition --- $condition")
        val conditionPattern = Regex("(.+?)\\s*(<=|>=|=|<|>)\\s*'(.+)'|(.+?)\\s*(<=|>=|=|<|>)\\s*(.+)")
        val matchResult = conditionPattern.matchEntire(condition) ?: return false

        val field = matchResult.groupValues[1].trim().ifEmpty { matchResult.groupValues[4].trim() }
        val operator = matchResult.groupValues[2].trim().ifEmpty { matchResult.groupValues[5].trim() }
        val value = matchResult.groupValues[3].trim().ifEmpty { matchResult.groupValues[6].trim() }

        val fieldValue = getValueFromJson(jsonNode, field)
        // println("CHK evaluateCondition --- $fieldValue $operator $value")

        return when (operator) {
            "=" -> fieldValue.toString() == value
            "<" -> compareValues(fieldValue.toString(), value) < 0
            ">" -> compareValues(fieldValue.toString(), value) > 0
            "<=" -> compareValues(fieldValue.toString(), value) <= 0
            ">=" -> compareValues(fieldValue.toString(), value) >= 0
            else -> false
        }
    }


    private fun isAlwaysTrueCondition(condition: String): Boolean {
        return condition.equals("1 = 1", ignoreCase = true) || condition.equals("'T' = 'T'", ignoreCase = true)
    }

    private fun getValueFromJson(jsonNode: JsonNode, path: String): Any {
        val pathParts = path.split(".")
        var currentNode: JsonNode = jsonNode
        for (part in pathParts) {
            currentNode = currentNode.get(part) ?: return ""
        }
        return when {
            currentNode.isArray -> currentNode.map {
                if (it.isObject) it.fields().asSequence().associate { it.key to it.value.asText() }
                else it.asText()
            }

            currentNode.isObject -> currentNode.fields().asSequence().associate { it.key to it.value.asText() }
            else -> currentNode.asText()
        }
    }

    private fun JsonNode.toRecordList(): List<JsonNode> {
        return if (this.isArray) {
            this.toList()
        } else {
            listOf(this)
        }
    }

    fun getKeys(depth: Int): List<String> {
        println("getKeys.depth: $depth")
        return if (rootNode.isArray && rootNode.size() > 0) {
            getLevelKeys(rootNode[0], depth)
        } else {
            getLevelKeys(rootNode, depth)
        }
    }

    private fun getLevelKeys(node: JsonNode, depth: Int, currentPath: String = ""): List<String> {
        if (depth == 0 || !node.isObject) {
            return listOf(currentPath.removePrefix("."))
        }
        val keys = mutableListOf<String>()
        node.fields().forEach { (key, value) ->
            val newPath = if (currentPath.isEmpty()) key else "$currentPath.$key"
            keys.addAll(getLevelKeys(value, depth - 1, newPath))
        }
        return keys
    }
}
