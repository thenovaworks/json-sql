package io.github.thenovaworks.json.query

import org.junit.jupiter.api.*
import kotlin.test.assertEquals

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class JsonQueryHandlerTest {

    @Test
    @Order(1)
    fun `test-convertStringToNumber`(): Unit {
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
    fun `test-query`(): Unit {
        val resultSet = SqlSession(
            JsonQueryHandler("HEALTH", json101)
        ).executeQuery("select id, detail.service, detail.statusCode, region from HEALTH")
        // println("resultSet: $resultSet")
        assertEquals("7bf73129-1428-4cd3-a780-95db273d1602", resultSet.map { v -> v.data[0]["id"] }.getOrNull())
    }

    @Test
    @Order(3)
    fun `test-query-with-parameter`(): Unit {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val sql = "select id, detail.service, detail.statusCode, region from HEALTH where id = :id"
        val record = sqlSession.queryForObject(sql, mapOf("id" to "7bf73129-1428-4cd3-a780-95db273d1602"))
        println("record: $record")
        assertEquals("7bf73129-1428-4cd3-a780-95db273d1602", record["id"])
    }

    @Test
    @Order(4)
    fun `test-query-with-conditions-and`(): Unit {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val params = mapOf(
            "id" to "7bf73129-1428-4cd3-a780-95db273d1602",
            // "statusCode" to "open"
            "statusCode" to "closed"
        )
        val sql =
            "select id, detail.service, detail.statusCode, region from HEALTH where id = :id and detail.statusCode = :statusCode"
        val record = sqlSession.queryForObject(sql, params)
        Assertions.assertEquals(0, record.size)
    }

    @Test
    @Order(5)
    fun `test-query-with-conditions-or`(): Unit {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val params = mapOf(
            "id" to "7bf73129-1428-4cd3-a780-95db273d1602",
            "source" to "aws.health",
            "statusCode" to "closed"
        )
        val sql = "select id, detail.service, detail.statusCode, region, detail.eventScopeCode from HEALTH " +
                "where id = :id or detail.statusCode = :statusCode"
        val record = sqlSession.queryForObject(sql, params)
        println("record: $record")
        Assertions.assertEquals("ELASTICLOADBALANCING", record.get("detail.service"))
        Assertions.assertEquals("open", record.get("detail.statusCode"))
        Assertions.assertEquals("PUBLIC", record.get("detail.eventScopeCode"))
    }

    @Test
    @Order(6)
    fun `test-query-with-conditions-multi`(): Unit {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val params = mapOf(
            "id" to "7bf73129-1428-4cd3-a780-95db273d1602",
            "source" to "aws.health",
            "statusCode" to "closed"
        )
        val sql =
            "select id, source, detail.service, detail.statusCode, region from HEALTH where id = :id and source = :source or detail.statusCode = :statusCode"
        val record = sqlSession.queryForObject(sql, params)
        println("record: $record")
        assertEquals("ap-southeast-2", record["region"])
    }

    @Test
    @Order(7)
    fun `test-query-102-with-conditions`(): Unit {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json102))
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
        val record = sqlSession.queryForObject(sql)
        println("record: $record")
        assertEquals("EC2", record["detail.service"])
        assertEquals("2023-01-27T01:43:21Z", record["time"])
    }

    @Test
    @Order(8)
    fun `test-query-102-with-condition-param`(): Unit {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json102))
        val params = mapOf(
            "id" to "26005bdb-b6eb-466d-920c-3ab19b1d7ea2",
            "source" to "aws.health",
            "statusCode" to "open"
        )
        val sql =
            "select time, time_01, resources, detail.affectedEntities, id, source, detail.service, detail.statusCode, region from HEALTH where id = :id and source = :source and detail.statusCode = :statusCode"
        val record = sqlSession.queryForObject(sql, params)
        println("record: $record")
    }

    @Test
    @Order(9)
    fun `test-users-101`(): Unit {
        val json = toJsonString("/users101.json")
        val sqlSession = SqlSession(JsonQueryHandler("USER", json))
        val sql = """
select  id, index, guid, isActive, balance, 
        age, eyeColor, name, gender, company, 
        email, phone, address, registered
from    USER 
where   id = :id
    """
        val records = sqlSession.queryForList(sql, mapOf("id" to "668feca3b450e6d8f583b561"))
        records.forEach(::println)
        println("keys: ${sqlSession.getKeys(2)}")
        assertEquals(1, records.size)
    }


    @Test
    @Order(10)
    fun `test-users-102-active`(): Unit {
        val json = toJsonString("/users102.json")
        val sqlSession = SqlSession(JsonQueryHandler("USER", json))
        val sql = """
select  id, index, guid, isActive, balance, 
        age, eyeColor, name, gender, company, 
        email, phone, address, registered
from    USER 
where   isActive = false
    """
        val records = sqlSession.queryForList(sql, mapOf("index" to 20))
        records.forEach(::println)
        assertEquals(13, records.size)
    }

    @Test
    @Order(11)
    fun `test-users-102-index`(): Unit {
        val json = toJsonString("/users102.json")
        val sqlSession = SqlSession(JsonQueryHandler("USER", json))
        val sql = """
select  id, index, guid, isActive, balance, 
        age, eyeColor, name, gender, company, 
        email, phone, address, registered
from    USER 
where   index > :index
    """
        val records = sqlSession.queryForList(sql, mapOf("index" to 20))
        records.forEach(::println)
        println("keys: ${sqlSession.getKeys(2)}")
        assertEquals(5, records.size)
    }

    @Test
    @Order(12)
    fun `test-users-102-age`(): Unit {
        val json = toJsonString("/users102.json")
        val sqlSession = SqlSession(JsonQueryHandler("USER", json))
        val sql = """
select  id, index, guid, isActive, balance, 
        age, eyeColor, name, gender, company, 
        email, phone, address, registered
from    USER 
where   age > 28 and age <= 30 
    """
        val records = sqlSession.queryForList(sql, mapOf("index" to 20))
        records.forEach(::println)
        // println("keys: ${sqlSession.getKeys(2)}")
        assertEquals(4, records.size)
    }


    @Test
    @Order(13)
    fun `test-users-103-where-params`(): Unit {
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
        val records = sqlSession.queryForList(sql, mapOf("gender" to "female", "age" to "30", "eyeColor" to "blue"))
        records.forEach(::println)
        // println("keys: ${sqlSession.getKeys(2)}")
        assertEquals(2, records.size)
    }

    @Test
    @Order(14)
    fun `test-health-104-result-map-utils`(): Unit {
        val json = toJsonString("/health104.json")
        val sqlSession = SqlSession(JsonQueryHandler("health", json))
        val sql = """
select  id, detail-type, source, account, time, region, resources, 
        detail.eventArn, detail.service, detail.eventTypeCode, detail.eventTypeCategory, detail.eventScopeCode, 
        detail.startTime, detail.lastUpdatedTime, detail.statusCode, detail.eventRegion, detail.eventDescription, 
        detail.affectedEntities, detail.affectedAccount
from    health
    """
        val rs = sqlSession.queryForObject(sql)
        val list = ResultMapUtils.toList(rs["detail.eventDescription"])
        val data = list.map { row ->
            ResultMapUtils.toMap(row)
        }.firstOrNull() // .forEach { println(it) }
        assertEquals("en_US", data?.get("language"))
    }

}
