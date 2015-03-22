package query

/**
 * Created by richard on 18.2.15.
 */
class Condition {
    String attribute
    Operator comparator

    String conditionString()    {
        attribute + " " + comparator.toString()
    }

    String toString()   {
        conditionString()
    }
}
