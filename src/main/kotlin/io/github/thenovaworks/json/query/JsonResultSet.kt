package io.github.thenovaworks.json.query

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDate.ofInstant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor

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

data class JsonResultMap(
    val map: Map<String, Any> = emptyMap()
) {

    companion object {

        @JvmStatic
        val datetimeFormatter = mapOf(
            """^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$""" to DateTimeFormatter.ISO_DATE_TIME,
            """^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}[+-]\d{2}:\d{2}$""" to DateTimeFormatter.ISO_DATE_TIME,
            "\\d{4}-\\d{2}-\\d{2}[+-]\\d{2}:\\d{2}" to DateTimeFormatter.ISO_OFFSET_DATE,
            "\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}" to DateTimeFormatter.ISO_OFFSET_TIME,
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}" to DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}" to DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}\\[.+]" to DateTimeFormatter.ISO_ZONED_DATE_TIME
        )

        @JvmStatic
        val dateFormatter = mapOf(
            """^\d{8}$""" to DateTimeFormatter.ofPattern("yyyyMMdd"),
            """^\d{4}-\d{2}-\d{2}$""" to DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            "\\d{4}-\\d{2}-\\d{2}[+-]\\d{2}:\\d{2}" to DateTimeFormatter.ISO_OFFSET_DATE,
            "\\d{4}-\\d{2}-\\d{2}" to DateTimeFormatter.ISO_DATE,
            "\\d{4}-\\d{3}" to DateTimeFormatter.ISO_ORDINAL_DATE,
            "\\d{4}-W\\d{2}-\\d" to DateTimeFormatter.ISO_WEEK_DATE
        )

        @JvmStatic
        private fun toLocalDateTime(value: String): LocalDateTime {
            // println("value:           $value")
            for ((pattern, formatter) in datetimeFormatter) {
                if (Regex(pattern).matches(value)) {
                    // println("matches: $pattern")
                    try {
                        val temporal: TemporalAccessor = formatter.parse(value)
                        // val localDateTime = ZonedDateTime.parse(value, formatter).toLocalDateTime()
                        // println("localDateTime: $localDateTime")
                        return when {
                            temporal.isSupported(java.time.temporal.ChronoField.OFFSET_SECONDS) -> {
                                // println("temporal isSupported: $temporal")
                                LocalDateTime.ofInstant(java.time.Instant.from(temporal), java.time.ZoneOffset.UTC)
                            }

                            else -> {
                                // println("temporal isNotSupported: $temporal ---")
                                LocalDateTime.from(temporal)
                            }
                        }
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("Error parsing date format: $value", e)
                    }
                }
            }
            throw IllegalArgumentException("Unsupported date format: $value")
        }

        @JvmStatic
        private fun toLocalDate(value: String): LocalDate {
            for ((pattern, formatter) in dateFormatter) {
                if (Regex(pattern).matches(value)) {
                    try {
                        val temporal: TemporalAccessor = formatter.parse(value)
                        return when {
                            temporal.isSupported(java.time.temporal.ChronoField.OFFSET_SECONDS) -> {
                                ofInstant(Instant.from(temporal), ZoneOffset.UTC)
                            }

                            else -> {
                                // println("temporal isNotSupported: $temporal ---")
                                LocalDate.from(temporal)
                            }
                        }
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("Error parsing date format: $value", e)
                    }
                }
            }
            throw IllegalArgumentException("Unsupported date format: $value")
        }
    }


    fun getString(columnName: String, defaultValue: String = ""): String {
        return map[columnName] as String? ?: defaultValue
    }

    fun getInt(columnName: String, defaultValue: Int = 0): Int = getString(columnName).toIntOrNull() ?: defaultValue

    fun getDateTime(columnName: String, defaultValue: LocalDateTime = LocalDateTime.MIN): LocalDateTime =
        (this.map[columnName] as String?)?.let { toLocalDateTime(it) }.apply { defaultValue } ?: defaultValue

    fun getDateTime(
        columnName: String,
        defaultValue: LocalDateTime = LocalDateTime.MIN,
        funcDateFormatter: (String) -> LocalDateTime = Companion::toLocalDateTime
    ): LocalDateTime? {
        val value = map[columnName] as String?
        return value?.let {
            funcDateFormatter(it)
        }.apply { defaultValue }
            ?: throw IllegalArgumentException("Value for $columnName is null or not a valid String")
    }

    fun getDate(columnName: String, defaultValue: LocalDate = LocalDate.MIN): LocalDate {
        return getString(columnName)?.let { toLocalDate(it) }.apply { defaultValue } ?: defaultValue
    }

    fun getDate(
        columnName: String, defaultValue: String = "", funcDateFormatter: (String) -> LocalDate = ::toLocalDate
    ): LocalDate? {
        val value = map[columnName] as String?
        return value?.let {
            funcDateFormatter(it)
        } ?: throw IllegalArgumentException("Value for $columnName is null or not a valid String")
    }

    @Suppress("UNCHECKED_CAST")
    fun getList(columnName: String): List<Map<String, Any>>? {
        val listVals = map[columnName]
        if (listVals == null || listVals is String) return null
        return listVals as List<Map<String, Any>>?
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

    fun isEmpty(): Boolean {
        return this.map.isEmpty()
    }

}
