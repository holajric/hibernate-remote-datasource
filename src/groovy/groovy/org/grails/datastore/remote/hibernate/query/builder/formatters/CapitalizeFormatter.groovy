package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import groovy.util.logging.Log4j

import java.beans.Introspector

/**
 * Created by richard on 1.4.15.
 */
@Log4j
class CapitalizeFormatter extends Formatter {
    Object format(Object input, List<Object> params) {
        if(!input)  {
            log.warn "Input not provided returning empty value"
            return ""
        }
        if(!params[0] || !(params[0] instanceof Boolean))  {
            log.warn "Invalid first parameter has to be true or false, returning unchanged input"
            return input
        }
        if(params[0] == true)
            return input.capitalize()
        else
            return Introspector.decapitalize(input)
    }
}
