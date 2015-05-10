package groovy.org.grails.datastore.remote.hibernate.parsers.calling

import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor

/**
 * This interface represents parsers for GORM API method callings,
 * it transforms them into query descriptors.
 */
public interface CallingParser {
    /**
     * This method parses static finder methods into query descriptors.
     * @param clazz name of class method is called on
     * @param finder name of method
     * @param params method params
     * @return parsed query descriptor
     */
    QueryDescriptor parseFinder(String clazz, String finder, params)

    /**
     * This method parses instance - modifying methods into query descriptors
     * @param operation type of operation
     * @param instance instance method is called on
     * @return parsed query descriptor
     */
    QueryDescriptor parseInstanceMethod(String operation, instance)
}