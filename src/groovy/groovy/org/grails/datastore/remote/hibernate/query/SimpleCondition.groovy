package groovy.org.grails.datastore.remote.hibernate.query

/**
 * Created by richard on 18.2.15.
 */
class SimpleCondition extends Condition {
    Object value

    String toString()   {
        attribute + " " + comparator.toString() + " " + value.toString()
    }
}
