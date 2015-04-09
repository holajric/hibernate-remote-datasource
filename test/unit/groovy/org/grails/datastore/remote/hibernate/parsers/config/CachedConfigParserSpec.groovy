package groovy.org.grails.datastore.remote.hibernate.parsers.config

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.util.Environment
import grails.util.Holders
import groovy.org.grails.datastore.remote.hibernate.auth.Authenticator
import groovy.org.grails.datastore.remote.hibernate.auth.TokenAuthenticator
import groovy.org.grails.datastore.remote.hibernate.connectors.DataSourceConnector
import groovy.org.grails.datastore.remote.hibernate.connectors.RestDataSourceConnector
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor
import groovy.org.grails.datastore.remote.hibernate.query.builder.QueryBuilder
import groovy.org.grails.datastore.remote.hibernate.query.builder.RestQueryBuilder
import groovy.org.grails.datastore.remote.hibernate.sync.MergingStrategy
import spock.lang.Specification
import development.Test
import development.RemoteTest
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class CachedConfigParserSpec extends Specification {

    def setupSpec() {
        ConfigObject currentConfig = grailsApplication.config
        ConfigSlurper slurper = new ConfigSlurper(Environment.getCurrent().getName());
        ConfigObject secondaryConfig = slurper.parse(grailsApplication.classLoader.loadClass("HibernateRemoteDatasourceDefaultConfig"))
        ConfigObject config = new ConfigObject();
        config.putAll(secondaryConfig.merge(currentConfig))
        grailsApplication.config = config;
    }

    def cleanup() {
    }

    @Unroll
    void "test if #givenClass is remote #isRemote"() {
        given:
        Class clazz = givenClass
        and:
        boolean result = CachedConfigParser.isRemote(clazz)
        grailsApplication.config.grails.plugins.hibernateRemoteDatasource.defaults.queryType = "Rest2"
        grailsApplication.config.grails.plugins.hibernateRemoteDatasource.defaults.sourceType = "SOAP"
        expect:
        assert result == isRemote
        assert CachedConfigParser.mapping?.get(clazz.getName())?."queryType" == expectedQueryType
        assert CachedConfigParser.mapping?.get(clazz.getName())?."sourceType" == expectedSourceType
        where:
        givenClass        | isRemote | expectedQueryType | expectedSourceType
        Test.class        | true     | "Rest"            | "Rest"
        Object.class      | false    | null              | null
        RemoteTest.class  | false    | null              | null

    }

    @Unroll
    void "test isRemote config change"() {
        given:
        Class clazz = Test.class
        and:
        Test?.remoteMapping?."sourceType" = "SOAP"
        Test?.remoteMapping?."queryType" = "Rest2"
        CachedConfigParser.mapping?.remove(clazz.getName())
        boolean result = CachedConfigParser.isRemote(clazz)
        expect:
        assert result
        assert CachedConfigParser.mapping?.get(clazz.getName())?."queryType" == "Rest2"
        assert CachedConfigParser.mapping?.get(clazz.getName())?."sourceType" == "SOAP"

    }

    @Unroll
    void "test if #givenDesc is valid descriptor #isValid"() {
        given:
        QueryDescriptor desc = givenDesc
        and:
        boolean result = CachedConfigParser.isValidDescriptor(desc)
        expect:
        assert result == isValid
        where:
        givenDesc                                                           | isValid
        null                                                                | false
        new QueryDescriptor()                                               | false
        new QueryDescriptor(entityName: "", operation: Operation.CREATE)    | false
        new QueryDescriptor(entityName: "Test", operation: Operation.CREATE)| true
        new QueryDescriptor(entityName: "Test")                             | false
    }

    @Unroll
    void "test if #expectedDataSource is returned for #givenClass with #givenSourceType"() {
        given:
        QueryDescriptor desc = new QueryDescriptor(entityName: givenClass, operation: Operation.CREATE)
        CachedConfigParser.mapping[givenClass] = ["sourceType": givenSourceType]
        and:
        DataSourceConnector result = CachedConfigParser.getDataSourceConnector(desc)
        expect:
        assert result?.getClass() == expectedDataSource?.getClass()
        where:
        givenClass          | givenSourceType  | expectedDataSource
        null                | "Test"           | null
        "development.Test"  | null             | null
        "development.Test"  | "asd"            | null
        "development.Test"  | "Rest"           | new RestDataSourceConnector()
    }

    void "test if #expectedQueryBuilder is returned for #givenClass with #givenQueryBuilder"() {
        given:
        QueryDescriptor desc = new QueryDescriptor(entityName: givenClass, operation: Operation.CREATE)
        CachedConfigParser.mapping[givenClass] = ["queryType": givenQueryBuilder]
        and:
        QueryBuilder result = CachedConfigParser.getQueryBuilder(desc)
        expect:
        assert result?.getClass() == expectedQueryBuilder?.getClass()
        where:
        givenClass          | givenQueryBuilder | expectedQueryBuilder
        null                | "Test"            | null
        "development.Test"  | null              | null
        "development.Test"  | "asd"             | null
        "development.Test"  | "Rest"            | new RestQueryBuilder()
    }

    @Unroll
    void "test if #givenOperation is #isAllowed for #givenClass with #givenAllowed"() {
        given:
        QueryDescriptor desc = new QueryDescriptor(entityName: givenClass, operation: givenOperation)
        CachedConfigParser.mapping[givenClass] = ["allowed": givenAllowed]
        and:
        boolean result = CachedConfigParser.isOperationAllowed(desc)
        expect:
        assert result == isAllowed
        where:
        givenClass          | givenOperation    | givenAllowed                       | isAllowed
        "development.Test"  | Operation.CREATE  | null                               | true
        "development.Test"  | Operation.READ    | null                               | true
        "development.Test"  | Operation.READ    | [Operation.READ, Operation.CREATE] | true
        "development.Test"  | Operation.UPDATE  | [Operation.READ, Operation.CREATE] | false
        "development.Test"  | Operation.UPDATE  | []                                 | false
    }

    @Unroll
    void "test if returns #expectedAuthenticator for #givenClass with #givenOperation and #givenMapping"() {
        given:
        QueryDescriptor desc = new QueryDescriptor(entityName: givenClass, operation: givenOperation)
        CachedConfigParser.authenticator["${desc.entityName} ${desc.operation}"] = null
        CachedConfigParser.mapping[givenClass] = givenMapping
        and:
        Authenticator result = CachedConfigParser.getAuthenticator(desc)
        expect:
        assert result?.getClass() == expectedAuthenticator?.getClass()
        where:
        givenClass          | givenOperation    | givenMapping                   | expectedAuthenticator
        "development.Test"  | Operation.CREATE  | null                           | null
        "development.Test"  | Operation.READ    | ["authentication": "Token"]    | new TokenAuthenticator("development.Test", Operation.READ)
        "development.Test"  | Operation.READ    | ["authentication": "TED"]      | null
        "development.Test"  | Operation.UPDATE  | ["operations": [
                                                   (Operation.READ):
                                                    ["authentication": "Token"],
                                                   (Operation.CREATE):
                                                    ["authentication":"Token"]]] | null
        "development.Test"  | Operation.CREATE  | ["operations": [
                                                   (Operation.READ):
                                                    ["authentication": "Token"],
                                                   (Operation.CREATE):
                                                    ["authentication":"Token"]]] | new TokenAuthenticator("development.Test", Operation.CREATE)
        "development.Test"  | Operation.CREATE  | ["operations": [
                                                   (Operation.READ):
                                                    ["authentication": "Token"],
                                                   (Operation.CREATE):
                                                    ["authentication":"TED"]],
                                                   "authentication": "Token"]    | null
        "development.Test"  | Operation.READ  | ["operations": [
                                                   (Operation.READ):
                                                    ["authentication": "Token"],
                                                   (Operation.CREATE):
                                                    ["authentication":"TED"]],
                                                 "authentication": "Token"]      | new TokenAuthenticator("development.Test", Operation.READ)
        "development.Test"  | Operation.UPDATE| ["operations": [
                                                   (Operation.READ):
                                                    ["authentication": "Token"],
                                                   (Operation.CREATE):
                                                    ["authentication":"TED"]],
                                                 "authentication": "Token"]      | new TokenAuthenticator("development.Test", Operation.UPDATE)
    }

    @Unroll
    void "test if authenticator has #expectedParams with #givenOperation and #givenMapping"() {
        given:
        QueryDescriptor desc = new QueryDescriptor(entityName: "development.Test", operation: givenOperation)
        CachedConfigParser.authenticator["development.Test ${desc.operation}"] = null
        CachedConfigParser.authenticationParams["development.Test ${desc.operation}"] = null
        CachedConfigParser.mapping["development.Test"] = givenMapping
        and:
        Authenticator result = CachedConfigParser.getAuthenticator(desc)
        expect:
        assert result?.@authenticationParameters == expectedParams
        where:
        givenOperation    | givenMapping                   | expectedParams
        Operation.READ    | ["authentication": "Token"]    | [:]
        Operation.READ    | ["authentication": "Token",
                            "authenticationParams": [
                                "token":"aasd1641161"
                            ]]                             | ["token":"aasd1641161"]
        Operation.READ  | ["authentication": "Token",
                           "authenticationParams": [
                                "token":"asdasdasd"
                           ],
                           "operations": [
                            (Operation.READ):
                             ["authentication": "Token"],
                            (Operation.CREATE):
                             ["authentication":"Token"]]] | ["token":"asdasdasd"]
        Operation.READ  | ["authentication": "Token",
                           "authenticationParams": [
                            "token":"aasd1641161"],
                           "operations": [
                            (Operation.READ):
                             ["authenticationParams": [
                               "token":"bf564asd6",
                               "bag":"asdad"]],
                            (Operation.CREATE):
                              ["authentication":"Token"]]] | ["token":"bf564asd6","bag":"asdad"]
    }

    @Unroll
    void "test if mapping is #expectedMapping with #givenClass and #givenMapping"() {
        given:
        QueryDescriptor desc = new QueryDescriptor(entityName: givenClass, operation: Operation.READ)
        CachedConfigParser.attributeMapping[givenClass] = null
        CachedConfigParser.mapping[givenClass] = givenMapping
        and:
        def result = CachedConfigParser.getAttributeMap(desc)
        expect:
        assert result == expectedMapping
        where:
        givenClass          | givenMapping             | expectedMapping
        "NotExist"          | []                       | null
        "java.lang.Object"  | []                       | null
        "development.Test"  | ["mapping": [
                                "name":"otherName"],
                               "local":[]]             | ["id":"id", "name":"otherName", "number":"number", "test":"test"]
        "development.Test"  | ["local":[]]             | ["id":"id", "name":"name", "number":"number", "test":"test"]
        "development.Test"  | ["mapping": [
                                "name":"otherName"],
                               "local":["number"]]     | ["id":"id", "name":"otherName", "test":"test"]
    }

    @Unroll
    void "test if operation is #expectedOperationMap with #givenClass, #givenOperation and #givenMapping"() {
        given:
        QueryDescriptor desc = new QueryDescriptor(entityName: givenClass, operation: givenOperation)
        CachedConfigParser.mapping[givenClass] = null
        CachedConfigParser.mapping[givenClass] = givenMapping
        and:
        def result = CachedConfigParser.getQueryOperation(desc)
        expect:
        assert result == expectedOperationMap
        where:
        givenClass          | givenOperation      | givenMapping                        | expectedOperationMap
        "development.Test"  | Operation.CREATE    | ["allowed":[Operation.READ]]        | null
        "development.Test"  | Operation.READ      | [:]                                 | ["endpoint" :"test/show/[:id]", "queryEndpoint":"test","mergingStrategy": MergingStrategy.PREFER_LOCAL, "method":"GET"]
        "development.Test"  | Operation.CREATE    | [:]                                 | ["endpoint" :"test/save", "mergingStrategy": MergingStrategy.PREFER_LOCAL, "method":"POST"]
        "development.Test"  | Operation.UPDATE    | [:]                                 | ["endpoint" :"test/update/[:id]", "mergingStrategy": MergingStrategy.PREFER_LOCAL, "method":"PUT"]
        "development.Test"  | Operation.UPDATE    | ["generalDefault":["method":"GET"]] | ["endpoint" :"test/update/[:id]", "mergingStrategy": MergingStrategy.PREFER_LOCAL, "method":"GET"]
        "development.Test"  | Operation.UPDATE    | ["operations":[(Operation.UPDATE):
                                                      ["method":"GET"]]]                | ["endpoint" :"test/update/[:id]", "mergingStrategy": MergingStrategy.PREFER_LOCAL, "method":"GET"]
        "development.Test"  | Operation.UPDATE    | ["generalDefault":["method":"POST"],
                                                     "operations":[(Operation.UPDATE):
                                                      ["method":"GET"]]]                | ["endpoint" :"test/update/[:id]", "mergingStrategy": MergingStrategy.PREFER_LOCAL, "method":"GET"]
    }
}
