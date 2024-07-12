# json-sql

This is easily access JSON messages through SQL query expressions.

## Usage

### Query Json and convert to Map type

```kotlin
    val json = """
  {
    "id": "668feca3b450e6d8f583b561",
    "index": 0,
    "guid": "704a409d-e95d-4aca-bd46-782d050a9b77",
    "isActive": false,
    "age": 39,
    "name": "Long Phillips",
    "email": "longphillips@imant.com",
    "registered": "2022-07-18T04:35:17 -09:00"
  }    
    """

val sql = "select id, guid, isActive, name, email from USER where id = '668ff4482965ffc5acb09c08'"
val sqlSession = SqlSession(JsonQueryHandler("USER", json))
val record = sqlSession.queryForObject(sql)
println("record: $record")
```

### Query Json and convert to List type

```kotlin
    val json = """
[
  {
    "id": "668ff4482965ffc5acb09c08",
    "index": 2,
    "guid": "dc9bb694-3a10-4005-9d91-c89bdca03ea3",
    "isActive": true,
    "age": 20,
    "name": "Manuela Olson",
    "email": "manuelaolson@zenthall.com",
    "registered": "2017-06-27T04:40:15 -09:00"
  },
  {
    "id": "668ff4482abed548eb94913f",
    "index": 3,
    "guid": "fb439ae2-5962-4eb9-8349-661687f3eb69",
    "isActive": true,
    "age": 31,
    "name": "Rodgers Carr",
    "email": "rodgerscarr@brainquil.com",
    "registered": "2019-01-30T03:59:21 -09:00"
  }
]    
    """

val sql = """
    select  id, index, guid, isActive, age, 
            name, email, registered
    from    USER 
    where   age > 28 and age <= 30 
    """
val sqlSession = SqlSession(JsonQueryHandler("USER", json))
val records = sqlSession.queryForList(sql, mapOf("index" to 20))
records.forEach(::println)
```

### Get the name of json attributes

Get the names of json properties by identifying them according to the hierarchy.


```kotlin
    val json = """
  {
    "id": "668feca3b450e6d8f583b561",
    "index": 0,
    "guid": "704a409d-e95d-4aca-bd46-782d050a9b77",
    "isActive": false,
    "age": 39,
    "name": "Long Phillips",
    "email": "longphillips@imant.com",
    "registered": "2022-07-18T04:35:17 -09:00",
    "detail": {
        "category": "issue",
        "code": "APPLE101",
        "communicationId": "1234abc01232a4012345678-1"
    }
  }    
    """

val sql = "select id, guid, isActive, name, email from USER where id = '668ff4482965ffc5acb09c08'"
val sqlSession = SqlSession(JsonQueryHandler("USER", json))

// Get the name of attributes only first depth 
println("Names for json attributes: ${sqlSession.getKeys()}")

// Get the name of attributes included second depth 
println("Names for json attributes included second depth: ${sqlSession.getKeys(2)}")
```


## What to do First?

To use [json-sql](https://github.com/thenovaworks/json-sql.git), simply add a dependency to your Maven or Gradle project.


- Maven
```xml
<dependencies>
    <dependency>
        <groupId>io.github.thenovaworks</groupId>
        <artifactId>json-sql</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```


- Gradle

```
dependencies {
    implementation 'io.github.thenovaworks:json-sql:1.0.0'
}
```


## Build

```
mvn clean package -DskipTests=true

# only install for local
mvn clean install -Dgpg.skip
```

## References

- [json-generator](https://json-generator.com/#)