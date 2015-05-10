package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import groovy.util.logging.Log4j

import java.text.ParseException

/**
 * Formatter that converts given Date in string form to other format in string
 */
@Log4j
class DateFormatter extends Formatter {
    /**
     * Converts input to date string in format given by first param
     * @param input date in string form
     * @param params [0] - format to convert in SimpleDateFormat
     * @return formatted date string
     */
    Object format(Object input, List<Object> params) {
        if(input == null)  {
            log.warn "Input not provided returning empty value"
            return ""
        }
        if(!params[0] || !(params[0] instanceof String))  {
            log.warn "Invalid first parameter has to String describing date format, returning unchanged input."
            return input
        }
        try {
            return (Date.parseToStringDate("$input")).format(params[0])
        } catch(ParseException ex)  {
            log.warn "Invalid input, returning unchanged input."
            return input
        }
    }
}
