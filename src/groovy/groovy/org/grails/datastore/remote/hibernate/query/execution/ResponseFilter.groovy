package groovy.org.grails.datastore.remote.hibernate.query.execution

import groovy.util.logging.Log4j
import groovy.org.grails.datastore.remote.hibernate.query.IntervalCondition
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor
import groovy.org.grails.datastore.remote.hibernate.query.SimpleCondition

/**
 * Created by richard on 22.3.15.
 */
@Log4j
class ResponseFilter {
    private static final List<String> ALLOWED_METHODS = ["inList", "lessThan", "lessThanEquals", "greaterThan", "greaterThanEquals", "like", "iLike", "rLike", "notEqual", "equals", "isNull", "isNotNull", "inRange", "between"]
    boolean isValid(instance, QueryDescriptor desc) {
        desc?.conditions?.each {
                String method = underscoreToCamelCase(it.comparator.toString())
                if(!ALLOWED_METHODS.contains(method))    {
                    log.info "Method $method not supported skipping"
                }   else if(it.attribute.empty)  {
                    log.info "Condition attribute is required"
                    return false
                }   else if(it instanceof IntervalCondition && (!it?.lowerBound || !it?.upperBound))   {
                    log.info "Condition lowerBound and upperBound are required for IntervalCondition"
                }   else if(it instanceof SimpleCondition && !it?.value)   {
                    log.info "Condition value is required for SimpleCondition"
                } else {
                    try {
                        if (it instanceof IntervalCondition) {
                            if (!this."$method"(instance?."${it.attribute}", it.lowerBound, it.upperBound))
                                return false
                        } else if (it instanceof SimpleCondition) {
                            if (!this."$method"(instance?."${it.attribute}", it.value))
                                return false
                        } else {
                            if (!this."$method"(instance?."${it.attribute}"))
                                return false
                        }
                    } catch(MissingPropertyException ex)   {
                        log.info "Attribute ${it.attribute} not found for ${instance}"
                    }
                }
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
