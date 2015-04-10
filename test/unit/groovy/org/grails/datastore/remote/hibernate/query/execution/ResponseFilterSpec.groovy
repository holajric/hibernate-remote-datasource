package groovy.org.grails.datastore.remote.hibernate.query.execution

import development.Test
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
import groovy.org.grails.datastore.remote.hibernate.query.builder.RestQueryBuilder
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class ResponseFilterSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    @Unroll
    void "test helper if method #givenMethodName is #expectedResult for #givenAttribute with #givenParamNum params #givenParams"() {
        given:
        String methodName = givenMethodName
        Object attribute = givenAttribute
        List<Object> params = givenParams
        ResponseFilter filter = new ResponseFilter()
        and:
        boolean result
        if(givenParamNum == 0)
            result = filter."$methodName"(attribute)
        else if(givenParamNum == 1)
            result = filter."$methodName"(attribute, params[0])
        else
            result = filter."$methodName"(attribute, params[0], params[1])
        expect:
        assert result == expectedResult
        where:
        givenMethodName     | givenAttribute | givenParamNum | givenParams         | expectedResult
        "inList"            | 1              | 1             | [[10,9,7]]          | false
        "inList"            | 1              | 1             | [[1,9,7]]           | true
        "lessThan"          | 1              | 1             | [0]                 | false
        "lessThan"          | 1              | 1             | [1]                 | false
        "lessThan"          | 1              | 1             | [2]                 | true
        "lessThanEquals"    | 1              | 1             | [0]                 | false
        "lessThanEquals"    | 1              | 1             | [1]                 | true
        "lessThanEquals"    | 1              | 1             | [2]                 | true
        "greaterThan"       | 1              | 1             | [0]                 | true
        "greaterThan"       | 1              | 1             | [1]                 | false
        "greaterThan"       | 1              | 1             | [2]                 | false
        "greaterThanEquals" | 1              | 1             | [0]                 | true
        "greaterThanEquals" | 1              | 1             | [1]                 | true
        "greaterThanEquals" | 1              | 1             | [2]                 | false
        "like"              | "AHOJ"         | 1             | [/A[A-Z]*J/]        | true
        "like"              | "AHOJ"         | 1             | [/a[a-z]*j/]        | false
        "iLike"             | "AHOJ"         | 1             | [/A[A-Z]*J/]        | true
        "iLike"             | "AHOJ"         | 1             | [/a[a-z]*j/]        | true
        "iLike"             | "AHOJ"         | 1             | [/.+A.*/]           | false
        "notEqual"          | "AHOJ"         | 1             | ["ahoj"]            | true
        "notEqual"          | 10             | 1             | [10]                | false
        "equals"            | "AHOJ"         | 1             | ["ahoj"]            | false
        "equals"            | 10             | 1             | [10]                | true
        "inRange"           | "c"            | 1             | ["a".."z"]          | true
        "inRange"           | 10             | 1             | [0..9]              | false
        "isNull"            | null           | 0             | []                  | true
        "isNull"            | "SOMETHING"    | 0             | []                  | false
        "isNotNull"         | null           | 0             | []                  | false
        "isNotNull"         | "SOMETHING"    | 0             | []                  | true
        "between"           | 1              | 2             | [0,10]              | true
        "between"           | 1              | 2             | [1,10]              | true
        "between"           | 1              | 2             | [2,10]              | false
    }

    @Unroll
    void "test if underscoreToCamelCase is #expectedResult for #givenText"() {
        given:
        String text = givenText
        ResponseFilter filter = new ResponseFilter()
        and:
        String result = filter.underscoreToCamelCase(text)
        expect:
        assert result == expectedResult
        where:
        givenText        | expectedResult
        null             | ""
        "    "           | ""
        "test"           | "test"
        "TEST"           | "test"
        "test_two"       | "testTwo"
        "TEST_TWO"       | "testTwo"
        "TEST_ONE_MORE"  | "testOneMore"
        "test_one_more"  | "testOneMore"
    }

    @Unroll
    void "test if #givenInstance is #isValid for #givenConditions and #givenConditionJoin"() {
        given:
        QueryDescriptor desc = new QueryDescriptor(entityName:"development.test", operation: Operation.READ, conditionJoin: givenConditionJoin, conditions: givenConditions)
        ResponseFilter filter = new ResponseFilter()
        Test instance = givenInstance
        and:
        boolean result = filter.isValid(instance,desc)
        expect:
        assert result == isValid
        where:
        givenInstance          | givenConditions                                         | givenConditionJoin  | isValid
        new Test(name:"test")  | [new Condition()]                                       | ConditionJoin.AND   | true
        new Test(name:"test")  | [new SimpleCondition(attribute:"name")]                 | ConditionJoin.AND   | true
        new Test(name:"test")  | [new SimpleCondition(attribute:"test",value:"test",
                                   comparator: Operator.EQUALS)]                         | ConditionJoin.AND   | true
        new Test(name:"test")  | [new IntervalCondition(attribute:"test",lowerBound:10)] | ConditionJoin.AND   | true
        new Test(name:"test")  | [new SimpleCondition(attribute:"name",value:"test2",
                                  comparator: Operator.EQUALS)]                          | ConditionJoin.AND   | false
        new Test(name:"test")  | [new SimpleCondition(attribute:"name",value:"test",
                                  comparator: Operator.EQUALS)]                          | ConditionJoin.AND   | true
        new Test(name:"test")  | [new SimpleCondition(attribute:"name",value:"test2",
                                  comparator: Operator.EQUALS)]                          | ConditionJoin.OR    | false
        new Test(name:"test")  | [new SimpleCondition(attribute:"name",value:"test",
                                  comparator: Operator.EQUALS)]                          | ConditionJoin.OR    | true
        new Test(name:"test")  | [new Condition(attribute:"name",
                                  comparator: Operator.IS_NOT_NULL)]                     | ConditionJoin.OR    | true
        new Test(name:"test")  | [new Condition(attribute:"number",
                                  comparator: Operator.IS_NOT_NULL)]                     | ConditionJoin.OR    | false
        new Test(number:10)    | [new IntervalCondition(attribute:"number",
                                  comparator: Operator.BETWEEN, lowerBound: 5,
                                   upperBound: 10)]                                      | ConditionJoin.OR    | true
        new Test(number:10)    | [new IntervalCondition(attribute:"number",
                                  comparator: Operator.BETWEEN, lowerBound: 5,
                                   upperBound: 9)]                                       | ConditionJoin.OR    | false
        new Test(name:"test")  | [new Condition(attribute:"number",
                                  comparator: Operator.IS_NOT_NULL), new SimpleCondition(
                                  attribute:"name",value:"test",
                                  comparator: Operator.EQUALS)]                          | ConditionJoin.OR    | true
        new Test(name:"test")  | [new Condition(attribute:"number",
                                  comparator: Operator.IS_NOT_NULL), new SimpleCondition(
                                  attribute:"name",value:"test",
                                  comparator: Operator.EQUALS)]                          | ConditionJoin.AND   | false

    }
}
