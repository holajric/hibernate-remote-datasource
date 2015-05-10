package groovy.org.grails.datastore.remote.hibernate.query

/**
 * Enumeration representing operators in dynamic finder, or
 * criterias for attribute to make condition satisfied.
 */
public enum Operator {
    IN_LIST,
    LESS_THAN_EQUALS,
    LESS_THAN,
    GREATER_THAN_EQUALS,
    GREATER_THAN,
    LIKE,
    ILIKE,
    NOT_EQUAL,
    IN_RANGE,
    RLIKE,
    BETWEEN,
    IS_NOT_NULL,
    IS_NULL,
    CONTAINS,
    EQUALS
}