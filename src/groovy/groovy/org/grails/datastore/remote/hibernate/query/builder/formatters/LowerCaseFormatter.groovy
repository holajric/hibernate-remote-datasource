package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import groovy.util.logging.Log4j

/**
 * Formatter that converts string to lowercase
 */
@Log4j
class LowerCaseFormatter extends Formatter {
    /**
     * Converts input string to lowercase
     * @param input string to be converted
     * @param params empty
     * @return input in lowercase
     */
    Object format(Object input, List<Object> params) {
        if(!input || !(input instanceof String))  {
            log.warn "Valid input not provided returning empty value"
            return ""
        }
        return input.toLowerCase()
    }
}
