package groovy.org.grails.datastore.remote.hibernate.query

/**
 * Enumeration representing relation between conditons
 * AND - All conditions must be satisfied
 * OR - At least one condition must be satisfied
 * NONE - There is only one or no condition
 */
public enum ConditionJoin {
    NONE,
    AND,
    OR
}