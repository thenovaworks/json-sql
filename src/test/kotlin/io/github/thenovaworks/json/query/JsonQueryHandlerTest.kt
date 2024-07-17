package io.github.thenovaworks.json.query

import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class JsonQueryHandlerTest {

    @Test
    @Order(1)
    fun `test-convertStringToNumber`() {
        val val1 = "1"
        val val2 = "23.0"

        val num1 = val1.toDoubleOrNull()
        val num2 = val2.toDoubleOrNull()

        if (num1 != null && num2 != null) {
            if (num1 < num2) {
                println("$val1 is less than $val2")
            } else if (num1 > num2) {
                println("$val1 is greater than $val2")
            } else {
                println("$val1 is equal to $val2")
            }
        } else {
            println("One of the values is not a valid number")
        }
    }

    private fun toJsonString(filepath: String): String {
        try {
            return object {}.javaClass.getResourceAsStream(filepath)?.bufferedReader().use { it?.readText() ?: "" }
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
            return ""
        }
    }

    private val json101 = toJsonString("/health101.json")

    private val json102 = toJsonString("/health102.json")

    @Test
    @Order(2)
    fun `test-query`() {
        val rs = SqlSession(
            JsonQueryHandler("HEALTH", json101)
        ).executeQuery("select id, detail.service, detail.statusCode, region from HEALTH")
        assertEquals("7bf73129-1428-4cd3-a780-95db273d1602", rs.map {
            it.getData().getString("id")
        }.getOrNull())
    }


    @Test
    @Order(3)
    fun `test-queryForObject-with-parameter`() {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val sql =
            "select id, detail.service, detail.statusCode, region from HEALTH where id = '7bf73129-1428-4cd3-a780-95db273d1602'"
        val rs = sqlSession.queryForObject(sql)
        assertEquals("7bf73129-1428-4cd3-a780-95db273d1602", rs.getString("id"))
    }

    @Test
    @Order(3)
    fun `test-queryForObject-TC101-with-parameter`() {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val sql = "select id, detail.service, detail.statusCode, region from HEALTH where id = :id"
        val rs = sqlSession.queryForObject(sql, mapOf("id" to "7bf73129-1428-4cd3-a780-95db273d1602"))
        println("rs: $rs")
        assertEquals("7bf73129-1428-4cd3-a780-95db273d1602", rs.getString("id"))
    }

    @Test
    @Order(4)
    fun `test-queryForObject-with-conditions`() {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val params = mapOf(
            "id" to "7bf73129-1428-4cd3-a780-95db273d1602", "statusCode" to "closed"
        )
        val sql =
            "select id, detail.service, detail.statusCode, region " + "from HEALTH " + "where id = :id " + "and detail.statusCode = :statusCode"
        val data = sqlSession.queryForObject(sql, params)
        println(data)
        assertTrue { data.isEmpty() }
        assertFalse { data.isNotEmpty() }

    }

    @Test
    @Order(5)
    fun `test-queryForObject-with-conditions-or`() {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val params = mapOf(
            "id" to "7bf73129-1428-4cd3-a780-95db273d1602", "source" to "aws.health", "statusCode" to "closed"
        )
        val sql =
            "select id, detail.service, detail.statusCode, region, detail.eventScopeCode from HEALTH " + "where id = :id or detail.statusCode = :statusCode"
        val rs = sqlSession.queryForObject(sql, params)
        println("record: $rs")
        assertEquals("ELASTICLOADBALANCING", rs.getString("detail.service"))
        assertEquals("open", rs.getString("detail.statusCode"))
        assertEquals("PUBLIC", rs.getString("detail.eventScopeCode"))
    }

    @Test
    @Order(6)
    fun `test-queryForObject-with-conditions-multi`() {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val params = mapOf(
            "id" to "7bf73129-1428-4cd3-a780-95db273d1602", "source" to "aws.health", "statusCode" to "closed"
        )
        val sql =
            "select id, source, detail.service, detail.statusCode, region from HEALTH where id = :id and source = :source or detail.statusCode = :statusCode"
        val rs = sqlSession.queryForObject(sql, params)
        println("record: $rs")
        assertEquals("ap-southeast-2", rs.getString("region"))
    }

    @Test
    @Order(7)
    fun `test-queryForObject-TC102-with-conditions`() {
        val sql = """
select  time,
        resources,
        id,
        source,
        detail.service,
        detail.statusCode,
        detail.affectedEntities,
        region
from    HEALTH
where   id = '26005bdb-b6eb-466d-920c-3ab19b1d7ea2'
and     source = 'aws.health'
    """
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json102))
        val rs = sqlSession.queryForObject(sql)
        println("rs: $rs")
        assertEquals("EC2", rs.getString("detail.service"))
        assertEquals("2023-01-27T01:43:21Z", rs.getString("time"))
    }

    @Test
    @Order(8)
    fun `test-queryForObject-TC102-with-params`() {
        val sql = """
select  time, time_01, resources,  id, source, region,
        detail.service, detail.statusCode, detail.affectedEntities
from    HEALTH where id = :id
and     source = :source
and     detail.statusCode = :statusCode
"""
        val params = mapOf(
            "id" to "26005bdb-b6eb-466d-920c-3ab19b1d7ea2", "source" to "aws.health", "statusCode" to "open"
        )
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json102))
        val record = sqlSession.queryForObject(sql, params)
        println("record: $record")
        // record.getOrDefault("", "")
    }

    @Test
    @Order(9)
    fun `test-queryForObject-TC101-users`() {
        val json = toJsonString("/users101.json")
        val sqlSession = SqlSession(JsonQueryHandler("USER", json))
        val sql = """
select  id, index, guid, isActive, balance,
        age, eyeColor, name, gender, company,
        email, phone, address, registered
from    USER
where   id = :id
    """
        val data = sqlSession.queryForObject(sql, mapOf("id" to "668feca3b450e6d8f583b561"))
        assertTrue(data.isNotEmpty())

        val age = data.getInt("age")
        val name = data.getString("name")
        val registered = data.getDate("registered")

        println("Age: $age")
        println("Name: $name")
        println("registered: $registered")
    }


    @Test
    @Order(10)
    fun `test-queryForList-TC102-users-active`() {
        val json = toJsonString("/users102.json")
        val sqlSession = SqlSession(JsonQueryHandler("USER", json))
        val sql = """
select  id, index, guid, isActive, balance,
        age, eyeColor, name, gender, company,
        email, phone, address, registered
from    USER
where   isActive = false
    """
        val list = sqlSession.queryForList(sql, mapOf("index" to 20))
        list.forEach { println(it) }
        assertEquals(13, list.size)
    }

    @Test
    @Order(11)
    fun `test-users-102-index`() {
        val json = toJsonString("/users102.json")
        val sqlSession = SqlSession(JsonQueryHandler("USER", json))
        val sql = """
select  id, index, guid, isActive, balance,
        age, eyeColor, name, gender, company,
        email, phone, address, registered
from    USER
where   index > :index
    """
        val list = sqlSession.queryForList(sql, mapOf("index" to "20"))
        list.forEach { println(it) }
        println("keys: ${sqlSession.getKeys(2)}")
        assertEquals(5, list.size)
    }

    @Test
    @Order(12)
    fun `test-users-102-age`() {
        val json = toJsonString("/users102.json")
        val sqlSession = SqlSession(JsonQueryHandler("USER", json))
        val sql = """
select  id, index, guid, isActive, balance,
        age, eyeColor, name, gender, company,
        email, phone, address, registered
from    USER
where   age > 28 and age <= 30
    """
        val list = sqlSession.queryForList(sql, mapOf("index" to "20"))
        list.forEach { println(it) }
        assertEquals(4, list.size)
    }


    @Test
    @Order(13)
    fun `test-users-103-where-params`() {
        val json = toJsonString("/users103.json")
        val sqlSession = SqlSession(JsonQueryHandler("member", json))
        val sql = """
select  index, guid, isActive, balance, age,
        eyeColor, name, gender, company, email,
        phone, address, registered, latitude, longitude,
        tags, friends, greeting, favoriteFruit
from    member
where   gender = :gender
and     age <= :age
and     eyeColor = :eyeColor
    """
        val list = sqlSession.queryForList(sql, mapOf("gender" to "female", "age" to "30", "eyeColor" to "blue"))
        list.forEach { println(it) }
        assertEquals(2, list.size)
    }

    @Test
    @Order(14)
    fun `test-health-104-result-map-utils`() {
        val json = toJsonString("/health104.json")
        val sqlSession = SqlSession(JsonQueryHandler("health", json))
        val sql = """
select  id, detail-type, source, account, time, region, resources,
        detail.eventArn, detail.service, detail.eventTypeCode, detail.eventTypeCategory, detail.eventScopeCode,
        detail.startTime, detail.lastUpdatedTime, detail.statusCode, detail.eventRegion, detail.eventDescription,
        detail.affectedEntities, detail.affectedAccount
from    health
    """
        val data = sqlSession.queryForObject(sql)
        val language = data.getList("detail.eventDescription")?.firstOrNull()?.get("language")
        assertEquals("en_US", language)
    }

}
