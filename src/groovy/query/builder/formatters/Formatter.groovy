package query.builder.formatters

import groovy.util.logging.Log4j

/**
 * Created by richard on 1.4.15.
 */
@Log4j
public abstract class Formatter {
    abstract Object format(Object input, List<Object> params)

    public static String formatAttribute(String url, String attribute, Object value)  {
        def matches = (url =~ /\[:${attribute}((\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)+)\]/)
        if(!matches)    {
            log.info "No formatters found in url ${url}"
            return value
        }
        for(int i = 0; i < matches.size();i++) {
            matches[i]?.getAt(1)?.substring(1)?.tokenize('|').each   {
                def formatterStruct = it.tokenize(':')
                Formatter formater
                try {
                    if (!((formater = Class.forName("query.builder.formatters.${formatterStruct[0].capitalize()}Formatter").newInstance()) instanceof Formatter))  {
                        log.info "Class query.builder.formatters.${formatterStruct[0].capitalize()}Formatter is not instance of query.builder.formatters.Formatter, this formatter will be skipped"
                    }
                    value = formater.format(value, formatterStruct.size > 1 ? formatterStruct[1..-1] : [])
                }   catch(ClassNotFoundException ex)    {
                    log.info "Class query.builder.formatters.${formatterStruct[0].capitalize()}Formatter does not exist, this formatter will be skipped"
                }
            }
        }
        return value
    }
}