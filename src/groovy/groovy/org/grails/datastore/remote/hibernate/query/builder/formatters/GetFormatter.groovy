package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import groovy.util.logging.Log4j

/**
 * Formatter that replace object by one of its attributes.
 */
@Log4j
class GetFormatter extends Formatter {
    /**
     * Gets attribute of object given by first parameter,
     * instead of object itself.
     * @param input - object
     * @param params [0] - attribute to be acquired
     * @return value of object attribute
     */
    Object format(Object input, List<Object> params) {
        if(!input)  {
            log.warn "Valid input not provided returning empty value"
            return ""
        }
        if(!params[0] || !(params[0] instanceof String))  {
            log.warn "Invalid first parameter has to String describing date format, returning unchanged input."
            return input
        }
        try {
            return input?."${params[0]}"
        } catch(MissingPropertyException ex)  {
            log.warn "Invalid input, returning unchanged input."
            return input
        }
    }
}
