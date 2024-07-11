package io.github.thenovaworks.json.query

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class JsonQueryHandlerTest {

    private val json101 = """
        {
          "version": "0",
          "id": "7bf73129-1428-4cd3-a780-95db273d1602",
          "detail-type": "AWS Health Event",
          "source": "aws.health",
          "account": "123456789012",
          "time": "2023-01-26T01:43:21Z",
          "region": "ap-southeast-2",
          "resources": [],
          "detail": {
            "eventArn": "arn:aws:health:ap-southeast-2::event/AWS_ELASTICLOADBALANCING_API_ISSUE_90353408594353980",
            "service": "ELASTICLOADBALANCING",
            "eventTypeCode": "AWS_ELASTICLOADBALANCING_OPERATIONAL_ISSUE",
            "eventTypeCategory": "issue",
            "eventScopeCode": "PUBLIC",
            "communicationId": "4826e1b01e4eed2b0f117c54306d907c713586799d76d487c9132a40149ac107-1",
            "startTime": "Thu, 26 Jan 2023 13:19:03 GMT",
            "endTime": "Thu, 26 Jan 2023 13:44:13 GMT",
            "lastUpdatedTime": "Thu, 26 Jan 2023 13:44:13 GMT",
            "statusCode": "open",
            "eventRegion": "ap-southeast-2",
            "eventDescription": [
              {
                "language": "en_US",
                "latestDescription": "A description of the event will be provided here"
              }
            ],
            "affectedAccount": "123456789012",
            "page": "1",
            "totalPages": "1"
          }
        }
    """

    private val json102 = """
        {
          "version": "0",
          "id": "26005bdb-b6eb-466d-920c-3ab19b1d7ea2",
          "detail-type": "AWS Health Event",
          "source": "aws.health",
          "account": "123456789012",
          "time": "2023-01-27T01:43:21Z",
          "region": "us-west-2",
          "resources": ["arn:ec2-1-101002929", "arn:ec2-1-101002930", "arn:ec2-1-101002931", "arn:ec2-1-101002932"],
          "detail": {
            "eventArn": "arn:aws:health:us-west-2::event/AWS_EC2_INSTANCE_STORE_DRIVE_PERFORMANCE_DEGRADED_90353408594353980",
            "service": "EC2",
            "eventTypeCode": "AWS_EC2_INSTANCE_STORE_DRIVE_PERFORMANCE_DEGRADED",
            "eventTypeCategory": "issue",
            "eventScopeCode": "ACCOUNT_SPECIFIC",
            "communicationId": "1234abc01232a4012345678-1",
            "startTime": "Thu, 27 Jan 2023 13:19:03 GMT",
            "lastUpdatedTime": "Thu, 27 Jan 2023 13:44:13 GMT",
            "statusCode": "open",
            "eventRegion": "us-west-2",
            "eventDescription": [{
              "language": "en_US",
              "latestDescription": "A description of the event will be provided here"
            }],
            "eventMetadata": {
              "keystring1": "valuestring1",
              "keystring2": "valuestring2",
              "keystring3": "valuestring3",
              "keystring4": "valuestring4",
              "truncated": "true"
            },
            "affectedEntities": [{
              "entityValue": "arn:ec2-1-101002929",
              "lastUpdatedTime": "Thu, 26 Jan 2023 19:01:55 GMT",
              "status": "IMPAIRED"
            }, {
              "entityValue": "arn:ec2-1-101002930",
              "lastUpdatedTime": "Thu, 26 Jan 2023 19:05:12 GMT",
              "status": "IMPAIRED"
            }, {
              "entityValue": "arn:ec2-1-101002931",
              "lastUpdatedTime": "Thu, 26 Jan 2023 19:07:13 GMT",
              "status": "UNIMPAIRED"
            }, {
              "entityValue": "arn:ec2-1-101002932",
              "lastUpdatedTime": "Thu, 26 Jan 2023 19:10:59 GMT",
              "status": "RESOLVED"
            }],
            "affectedAccount": "123456789012",
            "page": "1",
            "totalPages": "10"
          }
        }
    """

    @Test
    fun `test-query`(): Unit {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val record = sqlSession.queryForObject("select id, detail.service, detail.statusCode, region from HEALTH")
        println("record: $record")
    }

    @Test
    fun `test-query-with-parameter`(): Unit {
        val sqlSession = SqlSession(JsonQueryHandler("HEALTH", json101))
        val sql = "select id, detail.service, detail.statusCode, region from HEALTH where id = :id"
        val record = sqlSession.queryForObject(sql, mapOf("id" to "7bf73129-1428-4cd3-a780-95db273d1602"))
        println("record: $record")
    }

    @Test
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
    }

    @Test
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
    }

    @Test
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
}
