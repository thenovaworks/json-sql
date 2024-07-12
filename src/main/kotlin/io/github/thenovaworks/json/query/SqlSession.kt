package io.github.thenovaworks.json.query

import arrow.core.Either

class SqlSession(private val queryHandler: JsonQueryHandler) {

    fun executeQuery(query: String, params: Map<String, Any> = emptyMap()): Either<String, QueryResult> {
        return queryHandler.executeQuery(query, params)
    }

    fun queryForObject(query: String, params: Map<String, Any> = emptyMap()): Map<String, Any> {
        val resultSet = executeQuery(query, params)
        val record = resultSet.fold(
            { error -> throw RuntimeException(error) },
            { result ->
                result.data.firstOrNull() ?: throw RuntimeException("Data not found.")
            })
        return record
    }

    fun queryForList(query: String, params: Map<String, Any> = emptyMap()): List<Map<out Any, Any>> {
        val resultSet = executeQuery(query, params)
        val records = resultSet.fold(
            { error -> throw RuntimeException(error) },
            { result -> result.data.toList() })
        return records
    }

    fun getKeys(depth: Int = 1): List<String> {
        return queryHandler.getKeys(depth)
    }

}