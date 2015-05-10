package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import groovy.util.logging.Log4j

import java.beans.Introspector

/**
 * Formatter that capitalize or decapitalize string
 */
@Log4j
class CapitalizeFormatter extends Formatter {
    /**
     * Capitalize or decapitalize string based on first parameter.
     * @param input string to be formatted
     * @param params [0] - true = capitalize, false = decapitalize
     * @return
     */
    Object format(Object input, List<Object> params) {
        if(!input || !(input instanceof String))  {
            log.warn "Input not provided returning empty value"
            return ""
        }
        if(params[0] == null || (params[0]!="false" && params[0]!="true"))  {
            log.warn "Invalid first parameter has to be true or false, returning unchanged input"
            return input
        }
        if(params[0] == "true")
            return input.capitalize()
        else
            return Introspector.decapitalize(input)
    }
}
