package development

import query.IntervalCondition
import query.QueryDescriptor
import query.SimpleCondition

/**
 * Created by richard on 22.3.15.
 */
class ResponseFilter {
    boolean isValid(instance, QueryDescriptor desc) {
        desc.conditions.each {
            def method = underscoreToCamelCase(it.comparator.toString())
            if(it instanceof IntervalCondition)
                this."$method"(instance."${it.attribute}", it.lowerBound, it.upperBound)
            else if(it instanceof SimpleCondition)
                this."$method"(instance."${it.attribute}", it.value)
            else
                this."$method"(instance."${it.attribute}")
        }
        return true
    }

    boolean inList(attribute, list) {
        list.contains(attribute)
    }

    boolean lessThan(attribute, value) {
        attribute < value
    }

    boolean lessThanEquals(attribute, value) {
        attribute <= value
    }

    boolean greaterThan(attribute, value) {
        attribute > value
    }

    boolean greaterThanEquals(attribute, value) {
        attribute >= value
    }

    boolean like(attribute, regex)  {
        attribute ==~ regex
    }

    boolean iLike(attribute, regex)  {
        attribute.toUpperCase ==~ regex.toUpperCase
    }

    boolean rLike(attribute, regex)  {
        attribute ==~ regex
    }

    boolean notEqual(attribute, value) {
        attribute != value
    }

    boolean equals(attribute, value)   {
        attribute == value
    }

    boolean isNull(attribute)   {
        attribute == null
    }

    boolean isNotNull(attribute)   {
        attribute != null
    }

    boolean inRange(attribute, range)   {
        range.contains(attribute)
    }

    boolean between(attribute, lowerBound, upperBound)  {
        attribute >= lowerBound && attribute <= upperBound
    }

    String underscoreToCamelCase(String underscore){
        underscore = underscore.toLowerCase()
        if(!underscore || underscore.isAllWhitespace()){
            return ''
        }
        return underscore.replaceAll(/_\w/){ it[1].toUpperCase() }
    }

}
