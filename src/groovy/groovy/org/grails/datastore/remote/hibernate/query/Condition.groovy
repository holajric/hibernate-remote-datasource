package groovy.org.grails.datastore.remote.hibernate.query

/**
 * General condition that has only attribute and comparator (from operator enumeration),
 * it has no value to compare to attribute. Other conditions are inherited from this one.
 * General conditions are IS_NULL and IS_NOT_NULL, which takes no parameter.
 */
class Condition {
    /** Attribute that is subject of condition **/
    String attribute
    /** Criterium for attribute to meet condition **/
    Operator comparator

    /**
     * This method is used when is condition neccesary to be referenced as string
     * @return special string form of condition
     */
    String conditionString()    {
        attribute + " " + comparator.toString()
    }

    /**
     * toString alias for conditionString
     * @return same as conditionString
     */
    String toString()   {
        conditionString()
    }
}
