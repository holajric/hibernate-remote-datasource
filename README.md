# Hibernate remote datasource plugin

This plugin solves model tier level integration of web services based on REST
architecture into Grails applications. It extends existing implementation of datasource with synchronization functionality. 

Plugin works with most of REST resources and provides a simple API for integration with an application. This module significantly reduces the effort of developers with integrating web services and also decreases code redundancy. It also makes the whole process more effective thanks to saving remote data into local database and allows to use them with standard local domains. For accessing remote data it uses normal GORM syntax.  

Configuration is accesible in every domain class in remoteMapping static attribute.

## Usage

Domains using this plugin can be used same way as standart domains:

```groovy
User u = new User(name:"John", lastname:"Doe");
u.save();
User.findAllByName("John");
u.delete();
```

## Configuration

### Basic API
__baseUrl__ - remote data source URL

__sourceType__ - type of source; prefix of proper DataSourceConnector implementation, for instance RestDataSourceConnector

__queryType__ - request type accepted by source; prefix of proper QueryBuilder implementation, for instance RestQueryBuilder

__authentication__ - authentication type; prefix of proper Authenticator implementation, for instance TokenAuthenticator

__authenticationParams__ - authentication parameters, defined as pairs in format nameOfParameter : value; example of parameter is token

__local__ - list of local attributes, that are not part of remote source

__allowed__ - list of allowed operations from Operation enum (CREATE, READ,
UPDATE, DELETE); when this options is not set, all operations are allowed

__supportedParams__ - list of pagination/sorting parameters (max, offset, sort, order) supported by remote source; when this options is not set, all parameters are allowed

__paramMapping__ - mapping of pagination/sorting parameters to appropriate names of URL parameters on remote server; when mapping of parameter is not set, parameter is mapped to same named URL parameter; example is often mapping of local parameter max to URL parameter limit

__mapping__ - mapping of domain class attributes to remote source attributes, that means key in JSONObject response from remote source, that keeps value of attribute;
it allows to assign nested keys by using dot delimiter, for instance links.user;  when attribute mapping is not set, we suppose it has same name as attribute

__dataPrefix__ - common prefix that is used for all values of mapping settings, that means key of object, that encapsulates JSONObject representing domain object

__mappingTransformations__ - this option allows to assign transformation function to every attribute, this function accepts value from remote source as parameter, makes the proper transformation and returned result is then saved to attribute; example  of such function is transforming date from text format to object of type Date

__mergingStrategy__ - determines the way how data conflicts are solved, it is set to MergingStrategy enum value (FORCE_LOCAL, FORCE_REMOTE,
PREFER_LOCAL or PREFER_REMOTE)

__queryMapping__ - allows to map criterias to URL parameter it uses same syntax as endpoints with special parameter variables ([:value] for SimpleCondition, [:lowerBound] and [:upperBound] for IntervalCondition); example of such mapping is criterium date BETWEEN mapped to URL: from=[:lowerBound]&to=[:upperBound].

### Operation configuration

Every CRUD operation for domain can be configured independently. 

Also all attributes for operations can have assigned default values by using generalDefault configuration option. It can contain same attributes as operations, values in generalDefault are used in case that appropriate option for operation is not found. For general default option we use special variable [:operation] that contains name of executedoperation, we can also use formatters on it.

__method__ - HTTP method used for data source connection (GET,
POST, PUT, DELETE, ...)

__endpoint__ - setting of API endpoint URL, that is accesed in case of single item query; URL allows to use variable representing name of domain class attribute that is used for query definition and also allows to use formatters which are explained further in documentaion

__queryEndpoint__ - same as endpoint but it is used for queries on data collections

__(:prefix)Endpoint__ - similiar to endpoint, but this one is used, when value of prefix parameter is same as prefix of this configuration; for example URL saved in hashEndpoint is used when prefix has value hash

__(:prefix)QueryEndpoint__ - same as previous, but it is not alternative to endpoint, but to queryEndpoint

__endpoint (:kritérium)__ - this configuration is also similar to previous ones, difference is that this one is used in case, that query contains given criteria - for instance "endpoint name LIKE" is used in situation when query contains condition of type LIKE for attribute name; special variables based on condition type can be used in URL ([:value] for SimpleCondition, [:lowerBound] and [:upperBound] for IntervalCondition)

__authentication__ - allows to override basic API authentication for operation

__authenticationParams__ - allows to override basic API authenticationParams for operation

__mergingStrategy__ - allows to override basic API mergingStrategy for operation

### Examples
#### Minimal settings
```groovy
class Todo {
    String text
    static remoteMapping = [
        "baseUrl": "http://todo.com/api"
    ]
}
```
#### Advanced settings
```groovy
class Event {
    Long id
    String name
    String note
    Date starts_at
    Date ends_at
    int sequence_number
    String room
    int capacity
    ExchangeLockState exchangeLock = ExchangeLockState.ON_DEMAND
    String parallel
    String event_type
    
    static belongsTo = [course:Course]
    
    static hasMany = [teachers: Person, students: Person,
    bought: Exchange, sold: Exchange]
    
    static mappedBy = [bought:’bought’, sold: ’sold’]
    
    static mapping = {
        id generator:’assigned’
        course ignoreNotFound: true
    }
    
    static constraints = {
        teachers nullable:true
        students nullable:true
        parallel nullable:true
        course nullable:true
        room nullable:true
        bought nullable:true
        sold nullable:true
        note nullable:true
        name nullable:true
    }
    static remoteMapping = [
        "baseUrl": "https://sirius.fit.cvut.cz/api/v1/",
        "supportedParams": ["max", "offset"],
        "paramMapping": [
            "max": "limit"
        ],
        "local": ["bought", "sold", "exchangeLock"],
        "allowed": [Operation.READ],
        "mapping": [
            "room": "links.room",
            "course": "links.course",
            "students": "links.students",
            "teachers": "links.teachers"
        ],
        "dataPrefix":"events",
        "mappingTransformations": [
            "course": { course ->
                if(course == null)
                    return null
                Course courseTemp = Course.get(course) ?: new Course();
                courseTemp.code = course;
                courseTemp.id = course;
                courseTemp.save();
                return courseTemp
            },
            "ends_at": { date ->
                return Date.parse(’yyyy-MM-dd HH:mm:ss’,
                                  date.replaceAll(’T’, ’’)
                                  .replaceAll(’Z’,’’)
                                  .tokenize(’.’)[0])
            },
            "starts_at": { date ->
                ...
            },
            "students": { studentsList ->
                List<Person> persons = []
                studentsList.each { student ->
                    Person person = Person.get(
                                    student instanceof String ?
                                    student : String.valueOf(student)
                                    ) ?: new Person()
                    person.username = student instanceof String ?
                                      student : String.valueOf(student)
                    person.save()
                    persons << person
                }
                return persons
            },
            "teachers": { teachersList ->
                ...
            }
        ],
        "queryMapping": [
            "starts_at GREATER_THAN_EQUALS":
                "from=[:value|date<<yyyy-MM-dd’T’HH:mm:ss’Z’]",
            "ends_at LESS_THAN_EQUALS":
                "to=[:value|date<<yyyy-MM-dd’T’HH:mm:ss’Z’]",
            "course EQUALS":
                "course=[:value|get<<code]"
        ],
        "operations":[
            (Operation.READ):[
                "endpoint":
                    "events/[:id]?access_token=xxx",
                "queryEndpoint":
                    "events?access_token=xxx",
                "endpoint students CONTAINS":
                    "people/[:value|get<<username]/events?access_token=xxx",
                "endpoint teachers CONTAINS":
                    "people/[:value|get<<username]/events?access_token=xxx",
                "endpoint course EQUALS":
                    "courses/[:value|get<<code]/events?access_token=xxx",
                "endpoint room EQUALS":
                    "rooms/[:value]/events?access_token=xxx"
            ]
        ]
    ]
}
```

### Default settings
WILL BE ADDED LATER

### Formatters
WILL BE ADDED LATER