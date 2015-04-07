package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification
import spock.lang.Unroll

import java.beans.Introspector

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class CapitalizeFormatterSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    @Unroll
    void "#givenInput formatted by capitalizeFormatter with parameter #caseSwitchParam is #expectedResult"() {
        given:
        Object input = givenInput
        CapitalizeFormatter capitalizeFormatter = new CapitalizeFormatter()
        and:
        String result = capitalizeFormatter.format(input, [caseSwitchParam])
        expect:
        assert result == expectedResult
        where:
        givenInput      | expectedResult  | caseSwitchParam
        null            | ""              | "true"
        ""              | ""              | "false"
        10              | ""              | "true"
        "BIG"           | "BIG"           | ""
        "Capital"       | "Capital"       | "null"
        "small"         | "Small"         | "true"
        "More WORDS"    | "More WORDS"    | "true"
        "Capital"       | "capital"       | "false"
        "small"         | "small"         | "false"
        "More WORDS"    | "more WORDS"    | "false"
    }
}
