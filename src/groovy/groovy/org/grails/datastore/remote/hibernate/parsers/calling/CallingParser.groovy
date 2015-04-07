package groovy.org.grails.datastore.remote.hibernate.parsers.calling

import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor

/**
 * Created by richard on 18.2.15.
 */
public interface CallingParser {
    QueryDescriptor parseFinder(String clazz, String finder, params)
    QueryDescriptor parseInstanceMethod(String operation, instance)
}