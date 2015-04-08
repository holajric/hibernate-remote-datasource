package groovy.org.grails.datastore.remote.hibernate.parsers.calling

import development.Test
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import groovy.org.grails.datastore.remote.hibernate.query.Condition
import groovy.org.grails.datastore.remote.hibernate.query.ConditionJoin
import groovy.org.grails.datastore.remote.hibernate.query.IntervalCondition
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import groovy.org.grails.datastore.remote.hibernate.query.Operator
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor
import groovy.org.grails.datastore.remote.hibernate.query.SimpleCondition
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@Mock(Test)
@TestMixin(GrailsUnitTestMixin)
class GormApiParserSpec extends Specification {

    @Shared Test noId = new Test()
    @Shared Test withId = new Test()

    def setup() {
        withId.id = 1
    }

    def cleanup() {
    }

    @Unroll
    void "ParseInstanceMethod for operation #givenOperation on #givenInstance is #expectedDesc.operation with #expectedDesc.conditions.size()"() {
        given:
        String operation = givenOperation
        Object instance = givenInstance
        GormApiParser parser = new GormApiParser()
        and:
        QueryDescriptor desc = parser.parseInstanceMethod(operation, instance)
        expect:
        assert desc?.operation == expectedDesc?.operation
        assert desc?.entityName == expectedDesc?.entityName
        assert desc?.conditionJoin == expectedDesc?.conditionJoin
        assert desc?.paginationSorting == expectedDesc?.paginationSorting
        assert desc?.conditions?.size() == expectedDesc?.conditions?.size()
        if(desc?.conditions?.size() != 0 && desc?.conditions?.get(0)) {
            assert desc?.conditions?.get(0) instanceof SimpleCondition
            assert desc?.conditions?.get(0)?.attribute == expectedDesc?.conditions?.get(0)?.attribute
            assert desc?.conditions?.get(0)?.comparator == expectedDesc?.conditions?.get(0)?.comparator
            assert desc?.conditions?.get(0)?.value == expectedDesc?.conditions?.get(0)?.value
        }
        where:
        givenOperation    | givenInstance       | expectedDesc
        "create"          | null                | null
        "notOperation"    | noId                | null
        "create"          | noId                | new QueryDescriptor(operation: Operation.CREATE, entityName: "development.Test")
        "create"          | withId              | new QueryDescriptor(operation: Operation.CREATE, entityName: "development.Test")
        "update"          | noId                | new QueryDescriptor(operation: Operation.UPDATE, entityName: "development.Test")
        "update"          | withId              | new QueryDescriptor(operation: Operation.UPDATE, entityName: "development.Test", conditions: [new SimpleCondition(attribute: "id", comparator: Operator.EQUALS, value: 1)])
        "delete"          | noId                | new QueryDescriptor(operation: Operation.DELETE, entityName: "development.Test")
        "delete"          | withId              | new QueryDescriptor(operation: Operation.DELETE, entityName: "development.Test", conditions: [new SimpleCondition(attribute: "id", comparator: Operator.EQUALS, value: 1)])
        "create"          | new Object()        | new QueryDescriptor(operation: Operation.CREATE, entityName: "java.lang.Object")
    }

    @Unroll
    void "ParseFinder for class #givenClass and finder #givenFinder with params #givenParams is #expectedDesc"() {
        given:
        String clazz = givenClass
        String finder = givenFinder
        def params = givenParams
        GormApiParser parser = new GormApiParser()
        and:
        QueryDescriptor desc = parser.parseFinder(clazz, finder, params)
        expect:
        assert desc?.operation == expectedDesc?.operation
        assert desc?.entityName == expectedDesc?.entityName
        assert desc?.conditionJoin == expectedDesc?.conditionJoin
        assert desc?.paginationSorting == expectedDesc?.paginationSorting
        assert desc?.conditions?.size() == expectedDesc?.conditions?.size()
        if(desc?.conditions?.size() > 0)
            desc?.conditions?.eachWithIndex { condition, index ->
                assert condition?.attribute == expectedDesc?.conditions?.get(index)?.attribute
                assert condition?.comparator == expectedDesc?.conditions?.get(index)?.comparator
                if(expectedDesc?.conditions?.get(index) instanceof SimpleCondition) {
                    assert condition instanceof SimpleCondition
                    assert condition?.value == expectedDesc?.conditions?.get(index)?.value
                } else if(expectedDesc?.conditions?.get(index) instanceof IntervalCondition) {
                    assert condition instanceof IntervalCondition
                    assert condition?.lowerBound == expectedDesc?.conditions?.get(index)?.lowerBound
                    assert condition?.upperBound == expectedDesc?.conditions?.get(index)?.upperBound
                }   else    {
                    assert condition instanceof Condition
                }
            }
        where:
        givenClass         | givenFinder                             | givenParams                 | expectedDesc
        "development.Test" | null                                    | null                        | null
        "development.Test" | "notExist"                              | null                        | null
        "development.Test" | "find"                                  | null                        | new QueryDescriptor(operation: Operation.READ, entityName: "development.Test", paginationSorting: ["max": 1])
        "development.Test" | "findAll"                               | null                        | new QueryDescriptor(operation: Operation.READ, entityName: "development.Test")
        "development.Test" | "findAllById"                           | [1]                         | new QueryDescriptor(operation: Operation.READ, entityName: "development.Test", conditions: [new SimpleCondition(attribute: "id", comparator: Operator.EQUALS, value: 1)])
        "development.Test" | "findAllByIdBetween"                    | [1, 10]                     | new QueryDescriptor(operation: Operation.READ, entityName: "development.Test", conditions: [new IntervalCondition(attribute: "id", comparator: Operator.BETWEEN, lowerBound: 1, upperBound: 10)])
        "development.Test" | "findAllByIdBetweenAndName"             | [1, 10, "test", ["max":10]] | new QueryDescriptor(operation: Operation.READ, conditionJoin: ConditionJoin.AND, entityName: "development.Test", conditions: [new IntervalCondition(attribute: "id", comparator: Operator.BETWEEN, lowerBound: 1, upperBound: 10), new SimpleCondition(attribute: "name", comparator: Operator.EQUALS, value: "test")], paginationSorting: ["max": 10])
        "development.Test" | "findAllByIdGreaterThanOrNameIsNotNull" | [1]                         | new QueryDescriptor(operation: Operation.READ, conditionJoin: ConditionJoin.OR, entityName: "development.Test", conditions: [new SimpleCondition(attribute: "id", comparator: Operator.GREATER_THAN, value: 1), new Condition(attribute: "name", comparator: Operator.IS_NOT_NULL)])
    }
}
