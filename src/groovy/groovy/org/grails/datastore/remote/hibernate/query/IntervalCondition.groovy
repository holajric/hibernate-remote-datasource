package groovy.org.grails.datastore.remote.hibernate.query

/**
 * Class representing simple condition, that has two limiting values,
 * that are compared with given attribute, thus it represents interval.
 * For example BETWEEN condition.
 */
class IntervalCondition extends Condition {
    /** Lower bound of interval **/
    Object lowerBound
    /** Upper bound of interval **/
    Object upperBound

    /**
     * Simple string representation of condition
     * @return string representation of condition
     */
    String toString()   {
        attribute + " " + comparator.toString() + " " + lowerBound.toString() + " and " + upperBound.toString()
    }
}
