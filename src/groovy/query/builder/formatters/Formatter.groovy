package query.builder.formatters

/**
 * Created by richard on 1.4.15.
 */
public abstract class Formatter {
    abstract Object format(Object input, List<Object> params)

    public static String formatAttribute(String url, String attribute, Object value)  {
        def matches = (url =~ /\[:${attribute}((\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)+)\]/)
        if(matches)
            for(int i = 0; i < matches.size();i++) {
                matches[i]?.getAt(1)?.substring(1).tokenize('|').each   {
                    def formatterStruct = it.tokenize(':')
                    Formatter formater
                    try {
                        if ((formater = Class.forName("query.builder.formatters.${formatterStruct[0].capitalize()}Formatter").newInstance()) instanceof Formatter)
                            value = formater.format(value, formatterStruct.size > 1 ? formatterStruct[1..-1] : [])
                    }   catch(ClassNotFoundException ex)    {
                        //TODO invalid formatter
                    }
                }
            }
        return value
    }
}