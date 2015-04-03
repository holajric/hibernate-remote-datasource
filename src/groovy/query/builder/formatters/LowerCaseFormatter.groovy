package query.builder.formatters

import groovy.util.logging.Log4j

/**
 * Created by richard on 1.4.15.
 */
@Log4j
class LowerCaseFormatter extends Formatter {
    Object format(Object input, List<Object> params) {
        if(!input)  {
            log.info "Input not provided returning  empty value"
            return ""
        }
        return input.toLowerCase()
    }
}
