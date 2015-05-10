package groovy.org.grails.datastore.remote.hibernate.query.builder.formatters

import groovy.util.logging.Log4j

/**
 * Base class for all formatters, providing interface and static method
 * to delegate calling to proper formatters.
 */
@Log4j
public abstract class Formatter {
    /**
     * Formating method, executes formatter functionality itself.
     * @param input input to be formatted
     * @param params parameters for formatter
     * @return formatted input
     */
    abstract Object format(Object input, List<Object> params)

    /**
     * Static method that parses attribute from url and then executes its formatters
     * first one has given value as an input, others have output of previous formatter
     * calling. Method returns value of last formatter.
     * @param url given remote source URL
     * @param attribute attribute that should be parsed from URL
     * @param value value of attribute
     * @return attribute value after execution of all formatters
     */
    public static String formatAttribute(String url, String attribute, Object value)  {
        def matches = (url =~ /\[:${attribute}((\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)+)\]/)
        if(!matches)    {
            log.info "No formatters found in url ${url}"
            return value
        }
        for(int i = 0; i < matches.size();i++) {
            matches[i]?.getAt(1)?.substring(1)?.tokenize('|').each   {
                def formatterStruct = it.split('<<').toList()
                Formatter formater
                try {
                    if (!((formater = Class.forName("groovy.org.grails.datastore.remote.hibernate.query.builder.formatters.${formatterStruct[0].capitalize()}Formatter").newInstance()) instanceof Formatter))  {
                        log.warn "Class groovy.org.grails.datastore.remote.hibernate.query.builder.formatters.${formatterStruct[0].capitalize()}Formatter is not instance of groovy.org.grails.datastore.remote.hibernate.query.builder.formatters.Formatter, this formatter will be skipped"
                    }
                    value = formater.format(value, formatterStruct.size > 1 ? formatterStruct[1..-1] : [])
                }   catch(ClassNotFoundException ex)    {
                    log.warn "Class groovy.org.grails.datastore.remote.hibernate.query.builder.formatters.${formatterStruct[0].capitalize()}Formatter does not exist, this formatter will be skipped"
                }
            }
        }
        return value
    }
}