# json-sql

This is easily access JSON messages through SQL query expressions.

## Usage

```kotlin
    val json = """
  {
    "id": "668feca3b450e6d8f583b561",
    "index": 0,
    "guid": "704a409d-e95d-4aca-bd46-782d050a9b77",
    "isActive": false,
    "age": 39,
    "eyeColor": "brown",
    "name": "Long Phillips",
    "gender": "male",
    "company": "IMANT",
    "email": "longphillips@imant.com",
    "phone": "+1 (882) 456-3377",
    "address": "546 Pierrepont Place, Cetronia, Delaware, 1835",
    "registered": "2022-07-18T04:35:17 -09:00"
  }    
    """
    val sqlSession = SqlSession(JsonQueryHandler("USER", json))
    val record = sqlSession.queryForObject("select id, guid, isActive, name, email from USER")
    println("record: $record")
```

## What to do First?

## Build

```
 mvn clean package -DskipTests=true

```

## References

- [json-generator](https://json-generator.com/#)