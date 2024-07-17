package io.github.thenovaworks.json.query

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonResultMapTest {

    @Test
    fun `test-simple`() {
        val map = JsonResultMap(
            mapOf(
                "date" to "20240705",
                "dateStr" to "2024-07-15",
                "time" to "2024-07-05T19:00:31Z",
                "zonedTime" to "2018-10-25T08:00-04:00"
            )
        )

        assertEquals("2024-07-05", map.getDate("date").toString())
        assertEquals("2024-07-15", map.getDate("dateStr").toString())
        assertEquals("2024-07-05T19:00:31", map.getDateTime("time").toString())
        assertEquals("2018-10-25T12:00", map.getDateTime("zonedTime").toString())

        println(
            """
list:             ${map.getList("list")}
date:             ${map.getDate("date")}
dateStr:          ${map.getDate("dateStr")}
zonedTime:        ${map.getDateTime("zonedTime")}
time:             ${map.getDateTime("time")}
"""
        )
    }
}