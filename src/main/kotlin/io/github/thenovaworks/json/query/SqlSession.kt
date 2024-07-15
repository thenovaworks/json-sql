package io.github.thenovaworks.json.query

import arrow.core.Either

class SqlSession(private val queryHandler: JsonQueryHandler) {

    fun executeQuery(query: String, params: Map<String, Any> = emptyMap()): Either<String, JsonResultSet> {
        return queryHandler.executeQuery(query, params)
    }

    fun queryForObject(query: String, params: Map<String, Any> = emptyMap()): JsonResultMap {
        val rs = executeQuery(query, params)
        return rs.fold(
            { error -> throw RuntimeException(error) },
            { it.getData() }
        )
    }

    fun queryForList(query: String, params: Map<String, Any> = emptyMap()): List<JsonResultMap> {
        val rs = executeQuery(query, params)
        return rs.fold(
            { error -> throw RuntimeException(error) },
            { it.getList() })
    }

    fun getKeys(depth: Int = 1): List<String> {
        return queryHandler.getKeys(depth)
    }

}