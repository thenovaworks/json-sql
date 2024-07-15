package io.github.thenovaworks.json.query

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class JsonResultSet(val columns: List<String> = emptyList(), val data: List<JsonResultMap> = emptyList()) {

    fun getList(): List<JsonResultMap> {
        return data
    }

    fun getData(): JsonResultMap {
        return data.firstOrNull() ?: JsonResultMap()
    }

    fun size(): Int {
        return data.size
    }

}

data class JsonResultMap(val map: Map<String, Any> = emptyMap()) {

    companion object {
        @JvmStatic
        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss XXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss' 'XXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss' '"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss XXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'.' HH:mm:ss XXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'.' HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE
        )

        @JvmStatic
        fun toLocalDateTime(value: String): LocalDateTime {
            for (formatter in formatters) {
                try {
                    return when {
                        formatter == DateTimeFormatter.ISO_LOCAL_DATE -> LocalDate.parse(value, formatter)
                            .atStartOfDay()

                        else -> LocalDateTime.parse(value, formatter)
                    }
                } catch (e: DateTimeParseException) {
                    // Continue to the next formatter
                }
            }
            throw DateTimeParseException("Unable to parse date time: $value", value, 0)
        }
    }

    fun getString(columnName: String, defaultValue: String = ""): String {
        return map[columnName] as String? ?: defaultValue
    }

    fun getInt(columnName: String, defaultValue: Int = 0): Int {
        return (map[columnName] as String?)?.toIntOrNull() ?: defaultValue
    }


    fun getDate(columnName: String, defaultValue: LocalDateTime = LocalDateTime.MIN): LocalDateTime {
        return (map[columnName] as String?)?.let { toLocalDateTime(it) } ?: defaultValue
    }

    @Suppress("UNCHECKED_CAST")
    fun getList(columnName: String): List<Map<String, Any>>? {
        return map[columnName] as List<Map<String, Any>>?
    }

    fun get(columnName: String): Any? {
        return map[columnName]
    }

    /*
    @Suppress("UNUSED_EXPRESSION")
    fun <T> get(columnName: String, clazz: Class<T>): T? {
        val obj = map[columnName]
        return obj?.let {
            clazz.cast(obj)
        }?.also { null }
    }
    */

    fun keys(): Set<String> {
        return map.keys
    }

    fun values(): Collection<Any> {
        return map.values
    }

    fun isNotEmpty(): Boolean {
        return map.isNotEmpty()
    }

    fun isNullOrEmpty(): Boolean {
        return map.isNullOrEmpty()
    }

}
