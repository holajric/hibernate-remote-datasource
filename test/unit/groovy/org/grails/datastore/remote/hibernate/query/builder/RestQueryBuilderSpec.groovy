package groovy.org.grails.datastore.remote.hibernate.query.builder

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.util.Environment
import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
import groovy.org.grails.datastore.remote.hibernate.query.Condition
import groovy.org.grails.datastore.remote.hibernate.query.ConditionJoin
import groovy.org.grails.datastore.remote.hibernate.query.IntervalCondition
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import groovy.org.grails.datastore.remote.hibernate.query.Operator
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor
import groovy.org.grails.datastore.remote.hibernate.query.SimpleCondition
import groovy.org.grails.datastore.remote.hibernate.sync.MergingStrategy
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class RestQueryBuilderSpec extends Specification {

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
    void "test if isConditionSupported is #expectedResult with #givenClass, #givenCondition and #givenMapping"() {
        given:
        QueryDescriptor desc = new QueryDescriptor(entityName: givenClass, operation: Operation.CREATE)
        CachedConfigParser.mapping[givenClass] = null
        CachedConfigParser.mapping[givenClass] = givenMapping
        Condition cond = givenCondition
        RestQueryBuilder builder = new RestQueryBuilder()
        and:
        def result = builder.isConditionSupported(cond, desc)
        expect:
        assert result == expectedResult
        where:
        givenClass          | givenCondition                               | givenMapping                            | expectedResult
        "development.Test"  | new Condition()                              | ["queryMapping":["":"aa"]]              | false
        "development.Test"  | new Condition(attribute: "")                 | ["queryMapping":["":"aa"]]              | false
        "development.Test"  | new SimpleCondition(attribute: "name")       | ["queryMapping":["":"aa"]]              | false
        "development.Test"  | new IntervalCondition(attribute: "name")     | ["queryMapping":["":"aa"]]              | false
        "development.Test"  | new SimpleCondition(attribute: "name",
                              comparator: Operator.NOT_EQUAL, value:"Ahoj")| ["queryMapping":["":"aa"]]              | false
        "development.Test"  | new SimpleCondition(attribute: "name",
                              comparator: Operator.EQUALS, value:"AHOJ")   | ["queryMapping":["":"aa"]]              | true
        "development.Test"  | new Condition(attribute: "name",
                              comparator: Operator.IS_NULL)                | ["queryMapping":["name IS_NULL":"aa"]]  | true
        "development.Test"  | new SimpleCondition(attribute: "name",
                              comparator: Operator.NOT_EQUAL, value:"Ahoj")| ["queryMapping":["name NOT_EQUAL":"aa"]]| true
        "development.Test"  | new IntervalCondition(attribute: "number",
                              comparator: Operator.BETWEEN, lowerBound: 10,
                              upperBound: 30)                              | ["queryMapping":["number BETWEEN":"aa"]]| true
    }

    @Unroll
    void "test if isSingleQuery is #expectedResult with #givenQuery and #givenEndpoint"() {
        given:
        QueryDescriptor desc = givenQuery
        RestQueryBuilder builder = new RestQueryBuilder()
        and:
        def result = builder.isSingleQuery(desc, givenEndpoint)
        expect:
        assert result == expectedResult
        where:
        givenEndpoint   | givenQuery                                                 | expectedResult
        "[:name]"       | new QueryDescriptor(conditionJoin: ConditionJoin.AND)      | false
        "[:name]"       | new QueryDescriptor(conditionJoin: ConditionJoin.AND,
                                 conditions: [new Condition(attribute: "name",
                                 comparator: Operator.IS_NOT_NULL)])                 | false
        "[:name]"       | new QueryDescriptor(conditionJoin: ConditionJoin.AND,
                                 conditions: [new SimpleCondition(attribute: "name",
                                 comparator: Operator.NOT_EQUAL)])                   | false
        "[:name]"       | new QueryDescriptor(conditionJoin: ConditionJoin.AND,
                                 conditions: [new SimpleCondition(attribute: "name",
                                 comparator: Operator.EQUALS)])                      | false
        "[:test]"       | new QueryDescriptor(conditionJoin: ConditionJoin.NONE,
                                 conditions: [new SimpleCondition(attribute: "name",
                                 comparator: Operator.EQUALS)])                      | false
        "[:name]"       | new QueryDescriptor(conditionJoin: ConditionJoin.NONE,
                          conditions: [new SimpleCondition(attribute: "name",
                          comparator: Operator.EQUALS)])                             | true

    }

    @Unroll
    void "test if generateConditionQuery returns #expectedResult with #givenClass, #givenCondition and #givenMapping"() {
        given:
        QueryDescriptor desc = new QueryDescriptor(entityName: givenClass, operation: Operation.CREATE)
        CachedConfigParser.mapping[givenClass] = null
        CachedConfigParser.mapping[givenClass] = givenMapping
        Condition cond = givenCondition
        RestQueryBuilder builder = new RestQueryBuilder()
        and:
        def result = builder.generateConditionQuery(cond, desc)
        expect:
        assert result == expectedResult
        where:
        givenClass          | givenCondition                               | givenMapping                             | expectedResult
        "development.Test"  | new SimpleCondition(attribute: "name",
                              comparator: Operator.NOT_EQUAL, value:"Ahoj")| ["queryMapping":["":"aa"]]               | ""
        "development.Test"  | new SimpleCondition(attribute: "name",
                              comparator: Operator.EQUALS, value:"AHOJ")   | ["queryMapping":["":"aa"]]               | "name=AHOJ"
        "development.Test"  | new Condition(attribute: "name",
                              comparator: Operator.IS_NULL)                | ["queryMapping":[
                                                                               "name IS_NULL":"nameNull=true"]]       | "nameNull=true"
        "development.Test"  | new SimpleCondition(attribute: "name",
                              comparator: Operator.NOT_EQUAL, value:"Ahoj")| ["queryMapping":[
                                                                               "name NOT_EQUAL":"nameNot=[:value]"]]  | "nameNot=Ahoj"
        "development.Test"  | new IntervalCondition(attribute: "number",
                              comparator: Operator.BETWEEN, lowerBound: 10,
                              upperBound: 30)                              | ["queryMapping":[
                                                                               "number BETWEEN":"from=[:lowerBound]" +
                                                                                "&to=[:upperBound]"]]                | "from=10&to=30"
    }

    @Unroll
    void "test if generateBatchQuery returns #expectedResult with #givenOperation, #givenDescriptor, #givenMapping and givenPrefix"() {
        given:
        QueryDescriptor desc = givenDescriptor
        CachedConfigParser.mapping[desc.entityName] = null
        CachedConfigParser.mapping[desc.entityName] = givenMapping
        RestQueryBuilder builder = new RestQueryBuilder()
        and:
        def result = builder.generateBatchQuery(desc,givenOperation,givenPrefix)
        expect:
        assert result == expectedResult
        where:
        givenOperation                    | givenDescriptor                                       | givenMapping         | givenPrefix  | expectedResult
        ["endpoint" :
          "test/update/[:id]"]            | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.OR, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions: [
                                              new SimpleCondition(attribute: "name",
                                               comparator: Operator.EQUALS,
                                               value: "TEST")])                                   | [:]                  | ""           | ""
        ["endpoint" :
          "test/update/[:id]"]            | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.NONE, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions: [
                                              new SimpleCondition(attribute: "name",
                                               comparator: Operator.NOT_EQUAL,
                                               value: "TEST")])                                   | [:]                  | ""           | "test/update"
        ["endpoint" :
          "test/update/[:id]"]            | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.OR, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions:[])       | [:]                  | ""           | "test/update"
        ["queryEndpoint" :
          "test/queryUpdate",
         "endpoint" :
          "test/update/[:id]"]            | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.OR, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions:[])       | [:]                  | ""           | "test/queryUpdate"
        ["queryEndpoint" :
          "test/queryUpdate"]             | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.NONE, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions:[
                                              new SimpleCondition(attribute: "name",
                                               comparator: Operator.EQUALS,
                                               value: "TEST")])                                   | [:]                  | ""           | "test/queryUpdate?name=TEST"
        ["queryEndpoint" :
          "test/queryUpdate"]             | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.NONE, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions:[
                                              new SimpleCondition(attribute: "name",
                                               comparator: Operator.EQUALS,
                                               value: "TEST"),
                                              new Condition(attribute: "number",
                                               comparator: Operator.IS_NULL)])                    | [:]                  | ""           | "test/queryUpdate?name=TEST"
        ["queryEndpoint" :
          "test/queryUpdate"]             | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.NONE, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions:[
                                              new SimpleCondition(attribute: "name",
                                               comparator: Operator.EQUALS,
                                               value: "TEST"),
                                              new Condition(attribute: "number",
                                               comparator: Operator.IS_NULL)])                    | ["queryMapping":[
                                                                                                      "number IS_NULL":
                                                                                                      "numNull=true"]]   | ""           | "test/queryUpdate?name=TEST&numNull=true"
        [:]                               | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.OR, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions:[])       | [:]                  | ""           | ""
        ["endpoint" : "test/update"]      | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.OR, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions:[])       | [:]                  | "hash"       | ""
        ["hashEndpoint" : "test/update"]  | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.OR, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions:[])       | [:]                  | "hash"       | "test/update"
        ["endpoint" : "test/update"]      | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.OR, operation: Operation.UPDATE,
                                             entityName: "development.Test", paginationSorting: [
                                              "max":10, "sort":"name"])                           | ["supportedParams":
                                                                                                      ["max"]]            | ""           | "test/update?max=10"
        ["endpoint" : "test/update"]      | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.NONE, operation: Operation.UPDATE,
                                             entityName: "development.Test", conditions:[
                                              new SimpleCondition(attribute: "name",
                                               comparator: Operator.EQUALS,
                                               value: "TEST")], paginationSorting: [
                                              "max":10])                                          | [:]                  | ""           | "test/update?name=TEST&max=10"
        ["endpoint" : "test/update"]      | new QueryDescriptor(conditionJoin:
                                             ConditionJoin.OR, operation: Operation.UPDATE,
                                             entityName: "development.Test", paginationSorting: [
                                              "max":10, "sort":"name"])                           | ["paramMapping":
                                                                                                      ["max":"limit"]]  | ""           | "test/update?limit=10&sort=name"
    }

    @Unroll
    void "test if generateQuery returns #expectedResult with #givenDescriptor, #givenMapping and givenPrefix"() {
        given:
        QueryDescriptor desc = givenDescriptor
        CachedConfigParser.mapping[desc.entityName] = null
        CachedConfigParser.mapping[desc.entityName] = givenMapping
        RestQueryBuilder builder = new RestQueryBuilder()
        and:
        def result = builder.generateQuery(desc, givenPrefix)
        expect:
        assert result?.getClass() == expectedResult?.getClass()
        assert result?.url == expectedResult?.url
        assert result?.method == expectedResult?.method
        where:
        givenDescriptor                                       | givenMapping                    | givenPrefix  | expectedResult
        new QueryDescriptor(conditionJoin:
         ConditionJoin.NONE, operation: Operation.UPDATE,
         entityName: "", conditions: [
          new SimpleCondition(attribute: "name",
           comparator: Operator.EQUALS,
           value: "TEST")])                                   | ["baseUrl":"http://test.cz"]    | ""           | null
        new QueryDescriptor(conditionJoin:
         ConditionJoin.NONE, operation: Operation.UPDATE,
         entityName: "development.Test", conditions: [
          new SimpleCondition(attribute: "name",
           comparator: Operator.EQUALS,
           value: "TEST")])                                   | [:]                             | ""           | null
        new QueryDescriptor(conditionJoin:
         ConditionJoin.NONE, operation: Operation.UPDATE,
         entityName: "development.Test", conditions: [
          new SimpleCondition(attribute: "name",
           comparator: Operator.EQUALS,
           value: "TEST")])                                   | ["baseUrl":"http://test.cz",
                                                                 "allowed":[Operation.READ]]    | ""           | null
        new QueryDescriptor(conditionJoin:
         ConditionJoin.NONE, operation: Operation.UPDATE,
         entityName: "development.Test", conditions: [
          new SimpleCondition(attribute: "name",
           comparator: Operator.EQUALS,
           value: "TEST")])                                   | ["baseUrl":"http://test.cz"]    | "hash"       | null
        new QueryDescriptor(conditionJoin:
         ConditionJoin.NONE, operation: Operation.UPDATE,
         entityName: "development.Test", conditions: [
          new SimpleCondition(attribute: "name",
           comparator: Operator.EQUALS,
           value: "TEST")])                                   | ["baseUrl":"http://test.cz",
                                                                 "generalDefault":[
                                                                  "method": null]]              | ""           | null
        new QueryDescriptor(conditionJoin:
         ConditionJoin.AND, operation: Operation.READ,
         entityName: "development.Test", conditions: [
          new SimpleCondition(attribute: "name",
            comparator: Operator.EQUALS,
            value: "Test"), new SimpleCondition(
            attribute: "number", comparator: Operator.EQUALS,
            value: 10)])                                      | ["baseUrl":"http://test.cz/"]   | ""           | new RestRemoteQuery(url: "http://test.cz/test?name=Test&number=10", method: "GET")
    }

}
