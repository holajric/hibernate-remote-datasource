package groovy.org.grails.datastore.remote.hibernate.query.builder

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
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

    def setup() {
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
    void "test if isConditionSupported is #expectedResult with #givenQuery and #givenEndpoint"() {
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

    /*@Unroll
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
    }*/
}
