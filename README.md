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
    "gender": "male",
    "email": "longphillips@imant.com",
    "registered": "2022-07-18T04:35:17 -09:00"
  }    
    """
    val sqlSession = SqlSession(JsonQueryHandler("USER", json))
    val record = sqlSession.queryForObject("select id, guid, isActive, name, email from USER")
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
    "gender": "male",
    "email": "rodgerscarr@brainquil.com",
    "registered": "2019-01-30T03:59:21 -09:00"
  }
]    
    """
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
```

## What to do First?
[json-sql](https://github.com/thenovaworks/json-sql.git)를 사용 하려면 Maven 또는 Gradle 프로젝트에 디펜던시를 추가하면 됩니다.


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

```

## References

- [json-generator](https://json-generator.com/#)