package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import groovy.util.logging.Log4j

/**
 * Created by richard on 1.4.15.
 */
@Log4j
class DateFormatter extends Formatter {
    Object format(Object input, List<Object> params) {
        if(!input)  {
            log.info "Input not provided returning empty value"
            return ""
        }
        if(!params[0] || !(params[0] instanceof String))  {
            log.info "Invalid first parameter has to String describing date format, returning unchanged input."
            return input
        }
        return (new Date(input)).format(params[0])
    }
}
