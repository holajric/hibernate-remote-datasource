package query.builder.formatters

/**
 * Created by richard on 1.4.15.
 */
class DateFormatter extends Formatter {
    Object format(Object input, List<Object> params) {
        return (new Date(input)).format(params[0])
    }
}
