package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class LowerCaseFormatterSpec extends Specification {

    def setup() {

    }

    def cleanup() {
    }

    @Unroll
    void "#givenInput formatted by lowerCaseFormatter is #expectedResult"() {
        given:
            Object input = givenInput
            LowerCaseFormatter lowerCaseFormatter = new LowerCaseFormatter()
        and:
            String result = lowerCaseFormatter.format(input, [])
        expect:
            assert result == expectedResult
        where:
            givenInput      | expectedResult
            null            | ""
            ""              | ""
            new Object()    | ""
            "BIG"           | "big"
            "Capital"       | "capital"
            "small"         | "small"
            "More WORDS"    | "more words"
    }
}
