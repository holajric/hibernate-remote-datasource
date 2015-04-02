package query.builder.formatters

/**
 * Created by richard on 1.4.15.
 */
class LowerCaseFormatter extends Formatter {
    Object format(Object input, List<Object> params) {
        return input.toLowerCase()
    }
}
