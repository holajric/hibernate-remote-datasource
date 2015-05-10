package groovy.org.grails.datastore.remote.hibernate.query

/**
 * Class representing simple condition, that has one value,
 * that is compared with given attribute. For example EQUALS condition.
 */
class SimpleCondition extends Condition {
    /** Given condition value **/
    Object value

    /**
     * Simple string representation of condition
     * @return string representation of condition
     */
    String toString()   {
        attribute + " " + comparator.toString() + " " + value.toString()
    }
}
