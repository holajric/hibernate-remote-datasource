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

__authenticationParams__ - parametry pro autentifikaci, zapsané v podobě
dvojic ve tvaru názevParametru : hodnota; příklad parametru je token
local - seznam atributů, které jsou uloženy lokálně, a nejsou tedy součástí
vzdáleného zdroje

__allowed__ - seznam povolených operací z výčtu Operation (CREATE, READ,
UPDATE, DELETE); pokud není toto nastavení uvedeno, považují se za
povolené všechny čtyři operace

__supportedParams__ - seznam vzdáleným zdrojem povolených stránkovacích
a řadících parametrů (max, offset, sort,order); pokud není toto nastavení
uvedeno, považují se za povolené všechny čtyři parametry

__paramMapping__ - mapování stránkovacích a řadicích parametrů na odpovídající
názvy URL parametrů na vzdáleném serveru; pokud není parametr
a jeho mapování uvedeno, předpokládá se stejný název obou
parametrů; příkladem mapování může být časté mapování lokálního parametru
max na parametr limit

__mapping__ - mapování atributů doménové třídy na atributy vzdáleného zdroje,
respektive hodnota klíče, na kterém je hodnota odpovídajícího atributu
uložena v objektu typu JSONObject vráceném vzdáleným zdrojem;
umožňuje přiřadit vnořené klíče oddělováním názvů tečkou, např.
links.users; pokud nejsou atribut a jeho mapování uvedeny, předpokládá
se, že klíč má hodnotu názvu atributu

__dataPrefix__ - společný prefix pro všechny hodnoty nastavení mapping, respektive
klíč obalovacího objektu, ve kterém je skutečný JSONObject
reprezentující doménovou třídu obsažen

__mappingTransformations__ - toto nastavení umožňuje každému atributu při-
řadit transformační funkci, tj. funkci, která přijímá jako parametr odpovídající
hodnotu ze vzdáleného zdroje, provede příslušné transformace
a metodou vrácený výsledek je následně do atributu uložen; příkladem
může být převod textové podoby data na objekt typu Date

__mergingStrategy__ - nastavuje, jakým způsobem se řeší datové konflikty, obsahuje
hodnotu typu MergingStrategy(FORCE_LOCAL, FORCE_REMOTE,
PREFER_LOCAL nebo PREFER_REMOTE)

### Operation configuration

Every CRUD operation for domain can be configured independently. Mimo konfigurace operations lze v ní obsažené atributy nastavit globálně
v rámci celé doménové třídy pro všechny operace pomocí konfigurace
generalDefault, která obsahuje totožné možnosti nastavení. Konfigurace de-
finovaná v generalDefault se použije v případě, že není nalezeno příslušné
nastavení pro některou operaci. Aby bylo možno nastavit URL koncových
bodů univerzálně, umožňuje generalDefault použít v jejich konfiguraci speciální
proměnnou [:operation], do které je dosazen název volané operace, i
na tuto proměnnou je možno použít formattery.

__method__ - nastavení HTTP metody použité pro připojení ke zdroji (GET,
POST, PUT, DELETE, ...)

__endpoint__ - nastavení URL koncového bodu API, ke kterému se přistupuje
v případě dotazu na jednu položku; URL umožňuje použití proměnné
zastupující atribut doménové třídy použitý pro definování vyhledávacího
dotazu a zároveň umožňuje použití formatterů zmíněných v sekci 2.2.4.2

__queryEndpoint__ - obdobné jako endpoint s tím rozdílem, že tento koncový
bod API je určen pro dotazy na kolekce dat

__(:prefix)Endpoint__ - podobné jako endpoint, tento endpoint se použije v situaci,
kdy je hodnota parametru prefix při sestavování dotazu stejná
jako prefix této konfigurace; například URL uložené v hashEndpoint se
použije, pokud má prefix hodnotu hash

__(:prefix)QueryEndpoint__ - stejné jako předchozí konfigurace, ale slouží jako
alternativa pro queryEndpoint, a nikoliv endpoint

__endpoint (:kritérium)__ - i tato konfigurace je podobná předchozím, rozdílem
je, že tato je použita v situaci, kdy dotaz obsahuje dané kritérium - např.
endpoint name LIKE se použije v okamžiku, kdy dotaz obsahuje podmínku
typu LIKE pro atribut name; v URL lze používat na základě typu
podmínky speciální proměnné, pro podmínku typu SimpleCondition
je to proměnná [:value], pro IntervalCondition dvojice proměnných
[:lowerBound] a [:upperBound]

__authentication__ - totožné jako stejnojmenný atribut v hlavní konfiguraci, ale
umožňuje přepsat právě tento atribut pro konkrétní operaci

__authenticationParams__ - totožné jako stejnojmenný atribut v hlavní konfi-
guraci, ale umožňuje přepsat právě tento atribut pro konkrétní operaci

__mergingStrategy__ - totožné jako stejnojmenný atribut v hlavní konfiguraci,
ale umožňuje přepsat právě tento atribut pro konkrétní operaci


### Query mapping
Poslední nastavení, které nebylo zmíněno, je queryMapping, to umožňuje
mapovat kritéria na parametry URL, stejně jako koncový bod parametrizovaný
kritériem k tomu využívá příslušných speciálních proměnných. Příkladem
takového mapování může být kritérium date BETWEEN namapované na
část URL: from=[:lowerBound]&to=[:upperBound].

### Examples
```groovy
class Todo {
    String text
    static remoteMapping = [
        "baseUrl": "http://todo.com/api"
    ]
}
```

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
...
```

### Default settings
Výchozí konfigurace modulu je v rámci nastavení aplikace uložena v položce
grails.plugins.hibernateRemoteDatasource.defaults, ta obsahuje položky
sourceType a queryType, obě nastavené na hodnotu „Rest“. Položka
generalDefault určuje výchozí podobu pro endpoint a využívá pro to další
speciální proměnnou [:entityName], která je při zpracování nahrazena ná-
zvem třídy, výsledný koncový bod má podobu názevTřídy/názevoperace.
Nastavení generalDefault rovněž určuje jako výchozí MergingStrategy hodnotu
PREFER_LOCAL, tedy při řešení konfliktů je preferována lokální verze.
Poslední položkou výchozího nastavení je položka operations, ta v první
řadě definuje HTTP metody pro jednotlivé operace, GET pro READ, POST pro
CREATE, PUT pro UPDATE a DELETE pro DELETE. Mimo to pro CREATE definuje
mírně upravený endpoint, tj. v podobě názevTřídy/save, a pro READ jednak
dodefinovává queryEndpoint v podobě názevTřídy a jednak upravuje
endpoint do podoby názevTřídy/show/předanéId.
Tato nastavení lze globálně předefinovat v souboru Config.groovy v aplikaci,
která modul používá.