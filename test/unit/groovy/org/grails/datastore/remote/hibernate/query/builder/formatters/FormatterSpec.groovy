package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class FormatterSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    @Unroll
    void "#givenValue for attribute #givenAttribute formatted by Formatter from #givenUrl is #expectedResult"() {
        given:
        Object value = givenValue
        String attribute = givenAttribute
        String url = givenUrl
        and:
        String result = Formatter.formatAttribute(url, attribute, value)
        expect:
        assert result == expectedResult
        where:
        givenUrl                             | givenAttribute      | givenValue            | expectedResult
        "testUrl/[:test]"                    | "test"              | "small"               | "small"
        "testUrl"                            | "test"              | "small"               | "small"
        "testUrl/[:test|lowerCase]"          | "test"              | "BIG"                 | "big"
        "testUrl/[:test|lowerCase]"          | "else"              | "BIG"                 | "BIG"
        "testUrl/[:test|capitalize<<true]"    | "test"              | "small"               | "Small"
        "testUrl/[:test]/[:date|date<<Y-m-d]" | "date"              | new Date().toString() | new Date().format("Y-m-d")
    }
}
