package groovy.org.grails.datastore.remote.hibernate.query

/**
 * Created by richard on 18.2.15.
 */
class IntervalCondition extends Condition {
    Object lowerBound
    Object upperBound

    String toString()   {
        attribute + " " + comparator.toString() + " " + lowerBound.toString() + " and " + upperBound.toString()
    }
}
