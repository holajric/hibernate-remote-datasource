package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class DateFormatterSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    @Unroll
    void "#givenInput formatted by dateFormatter with parameter #format is #expectedResult"() {
        given:
        Object input = givenInput
        DateFormatter dateFormatter = new DateFormatter()
        and:
        String result = dateFormatter.format(input.toString(), [format])
        expect:
        assert result == expectedResult
        where:
        givenInput                      | expectedResult  | format
        new Date().copyWith(year: 2014,
                month: Calendar.OCTOBER,
                dayOfMonth: 2)          | "2014-10-2"     | "Y-M-d"
    }
}
