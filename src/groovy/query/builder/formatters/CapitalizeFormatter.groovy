package query.builder.formatters

import java.beans.Introspector

/**
 * Created by richard on 1.4.15.
 */
class CapitalizeFormatter extends Formatter {
    Object format(Object input, List<Object> params) {
        if(params[0] == true)
            return input.capitalize()
        else
            return Introspector.decapitalize(input)
    }
}
