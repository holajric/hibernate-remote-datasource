package groovy.org.grails.datastore.remote.hibernate.query.execution

import groovy.org.grails.datastore.remote.hibernate.query.ConditionJoin
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
        boolean isValid = desc.conditionJoin != ConditionJoin.OR
        desc?.conditions?.find {
                String method = underscoreToCamelCase(it.comparator.toString())
                if(!ALLOWED_METHODS.contains(method))    {
                    log.warn "Method $method not supported skipping"
                }   else if(!it.attribute || it.attribute.empty)  {
                    log.warn "Condition attribute is required"
                }   else if(it instanceof IntervalCondition && (!it?.lowerBound || !it?.upperBound))   {
                    log.warn "Condition lowerBound and upperBound are required for IntervalCondition"
                }   else if(it instanceof SimpleCondition && !it?.value)   {
                    log.warn "Condition value is required for SimpleCondition"
                } else {
                    try {
                        if (it instanceof IntervalCondition) {
                            if ((desc.conditionJoin == ConditionJoin.AND || desc.conditionJoin == ConditionJoin.NONE) && instance?."${it.attribute}" && !this."$method"(instance?."${it.attribute}", it.lowerBound, it.upperBound))    {
                                isValid = false
                                return true
                            }
                            if (desc.conditionJoin == ConditionJoin.OR && this."$method"(instance?."${it.attribute}", it.lowerBound, it.upperBound))    {
                                isValid = true
                                return true
                            }
                        } else if (it instanceof SimpleCondition) {
                            if ((desc.conditionJoin == ConditionJoin.AND || desc.conditionJoin == ConditionJoin.NONE) && instance?."${it.attribute}" && !this."$method"(instance?."${it.attribute}", it.value))    {
                                isValid = false
                                return true
                            }
                            if (desc.conditionJoin == ConditionJoin.OR && this."$method"(instance?."${it.attribute}", it.value))    {
                                isValid = true
                                return true
                            }
                        } else {
                            if ((desc.conditionJoin == ConditionJoin.AND || desc.conditionJoin == ConditionJoin.NONE) && !this."$method"(instance?."${it.attribute}"))    {
                                isValid = false
                                return true
                            }
                            if (desc.conditionJoin == ConditionJoin.OR && this."$method"(instance?."${it.attribute}"))    {
                                isValid = true
                                return true
                            }
                        }
                    } catch(MissingPropertyException ex)   {
                        log.warn "Attribute ${it.attribute} not found for ${instance}"
                        return
                    }
                }
        }
        return isValid
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
        attribute.toUpperCase() ==~ regex.toUpperCase()
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
        if(underscore?.getClass() != String || underscore.isAllWhitespace()){
            return ""
        }
        underscore = underscore.toLowerCase()
        return underscore.replaceAll(/_\w/){ it[1].toUpperCase() }
    }

}
