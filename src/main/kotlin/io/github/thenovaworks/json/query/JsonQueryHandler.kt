package io.github.thenovaworks.json.query

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

data class QueryResult(val columns: List<String>, val data: List<Map<String, Any>>)

class JsonQueryHandler(private val schemaName: String, private val json: String) {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val rootNode: JsonNode = objectMapper.readTree(json)

    private fun checkForUnboundParameters(condition: String) {
        val unboundParameterPattern = Regex(":\\w+")
        val matchResult = unboundParameterPattern.find(condition)
        if (matchResult != null) {
            throw IllegalArgumentException("Unbounded parameter error. `${matchResult.value}`")
        }
    }

    private fun substituteParams(condition: String, params: Map<String, Any>): String {
        var result = condition
        params.forEach { (key, value) ->
            result = result.replace(":$key", "'$value'")
        }
        return result
    }

    private fun filterRecords(jsonNode: JsonNode, whereCondition: String): List<JsonNode> {
        return if (evaluateConditions(jsonNode, whereCondition)) {
            listOf(jsonNode)
        } else {
            emptyList()
        }
    }

    private fun evaluateConditions(jsonNode: JsonNode, whereCondition: String): Boolean {
        val tokens = tokenizeCondition(whereCondition)
        println("tokens: ${tokens.size}")
        println("tokens: ${tokens}")
        return evaluateTokens(jsonNode, tokens)
    }

    private fun tokenizeCondition(condition: String): List<String> {
        val regex = Regex("(?i)\\s*(and|or)\\s*|\\s*([^\\s]+\\s*=\\s*'[^']+')\\s*")
        return regex.findAll(condition).map { it.value.trim() }.toList()
    }

    private fun evaluateTokens(jsonNode: JsonNode, tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return true

        var result = evaluateCondition(jsonNode, tokens[0])
        var index = 1

        while (index < tokens.size) {
            val operator = tokens[index].lowercase(Locale.getDefault())
            val nextCondition = tokens[index + 1]

            println("CHK operator: $operator")

            when (operator) {
                "and" -> result = result && evaluateCondition(jsonNode, nextCondition)
                "or" -> result = result || evaluateCondition(jsonNode, nextCondition)
            }
            index += 2
        }

        return result
    }

    private fun evaluateCondition(jsonNode: JsonNode, condition: String): Boolean {
        val conditionPattern = Regex("(.+?)\\s*=\\s*'(.+)'")
        val matchResult = conditionPattern.matchEntire(condition) ?: return false
        val field = matchResult.groupValues[1].trim()
        val value = matchResult.groupValues[2].trim()
        val attrVal = getValueFromJson(jsonNode, field)
        return value == attrVal
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


    fun executeQuery(query: String, params: Map<String, Any>): Either<String, QueryResult> {
        return try {
//            val selectPattern = Regex("select (.+) from $schemaName(?: where (.*))?", RegexOption.IGNORE_CASE)
//            val matchResult = selectPattern.matchEntire(query.trim())
//                ?: return "Invalid query format".left()
//            println("matchResult: $matchResult")
            val selectPattern =
                Regex("(?i)select\\s+(.+)\\s+from\\s+$schemaName(?:\\s+where\\s+(.+))?", RegexOption.DOT_MATCHES_ALL)
            val matchResult = selectPattern.matchEntire(query.trim())
                ?: return "Invalid query format".left()

            val columns = matchResult.groupValues[1].split(",").map { it.trim() }
            val whereCondition = matchResult.groupValues.getOrNull(2)?.trim()
                ?.let { substituteParams(it, params) }
                ?.also { checkForUnboundParameters(it) }

            println("CHK whereCondition: $whereCondition")

            val filteredRecords = if (whereCondition.isNullOrEmpty() || isAlwaysTrueCondition(whereCondition)) {
                listOf(rootNode)
            } else {
                filterRecords(rootNode, whereCondition)
            }

            val results = filteredRecords.map { record ->
                columns.associateWith { column ->
                    getValueFromJson(record, column)
                }
            }

            QueryResult(columns, results).right()
        } catch (e: Exception) {
            // e.printStackTrace()
            e.message.orEmpty().left()
        }
    }
}

class SqlSession(private val queryHandler: JsonQueryHandler) {

    fun executeQuery(query: String, params: Map<String, Any> = emptyMap()): Either<String, QueryResult> {
        return queryHandler.executeQuery(query, params)
    }

    fun queryForObject(query: String, params: Map<String, Any> = emptyMap()): Map<String, Any> {
        val resultSet = executeQuery(query, params)
        val record = resultSet.fold(
            { error ->
                mapOf("ERROR" to error)
            },
            { result ->
                result.data.firstOrNull() ?: emptyMap()
            }
        )
        return record
    }

    fun queryForList(resultSet: Either<String, QueryResult>): List<Map<out Any, Any>> {
        val records = resultSet.fold(
            { _ ->
                //mapOf("ERROR" to $it[0])
                emptyList<Map<Any, Any>>()
            },
            { result ->
                result.data.toList()
            }
        )
        return records
    }
}
