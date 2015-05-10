package groovy.org.grails.datastore.remote.hibernate.query.execution

import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
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
    /**
     * List of allowed methods - operation with attribute
     */
    private static final List<String> ALLOWED_METHODS = ["inList", "lessThan", "lessThanEquals", "greaterThan", "greaterThanEquals", "like", "iLike", "rLike", "notEqual", "equals", "isNull", "isNotNull", "inRange", "between", "contains"]

    /**
     * Checks if given instance met all descriptor conditions,
     * so if it is valid.
     * @param instance instance to be validated
     * @param desc descriptor of executed query
     * @return if given instance is valid by query descriptor
     */
    boolean isValid(instance, QueryDescriptor desc) {
        boolean isValid = desc.conditionJoin != ConditionJoin.OR
        desc?.conditions?.find {
            String method = underscoreToCamelCase(it.comparator.toString())
            if(!ALLOWED_METHODS.contains(method))    {
                log.warn "Method $method not supported skipping"
            }   else if(!it?.attribute || it?.attribute.empty)  {
                log.warn "Condition attribute is required"
            }   else if(it instanceof IntervalCondition && (!it?.lowerBound || !it?.upperBound))   {
                log.warn "Condition lowerBound and upperBound are required for IntervalCondition"
            }   else if(it instanceof SimpleCondition && !it?.value)   {
                log.warn "Condition value is required for SimpleCondition"
            } else {
                try {
                    def response = instance?."${it.attribute}"
                    if(CachedConfigParser.mapping[desc.entityName]?."mappingTransformations" && CachedConfigParser.mapping[desc.entityName]["mappingTransformations"][it.attribute]) {
                        response = CachedConfigParser.mapping[desc.entityName]["mappingTransformations"][it.attribute](instance?."${it.attribute}")
                    }
                    if (it instanceof IntervalCondition) {
                        if ((desc.conditionJoin == ConditionJoin.AND || desc.conditionJoin == ConditionJoin.NONE) && response && !this."$method"(response, it?.lowerBound, it?.upperBound))    {
                            isValid = false
                            return true
                        }
                        if (desc.conditionJoin == ConditionJoin.OR && this."$method"(response, it?.lowerBound, it?.upperBound))    {
                            isValid = true
                            return true
                        }
                    } else if (it instanceof SimpleCondition) {
                        if ((desc.conditionJoin == ConditionJoin.AND || desc.conditionJoin == ConditionJoin.NONE) && response && !this."$method"(response, it?.value))    {
                            isValid = false
                            return true
                        }
                        if (desc.conditionJoin == ConditionJoin.OR && this."$method"(response, it?.value))    {
                            isValid = true
                            return true
                        }
                    } else {
                        if ((desc.conditionJoin == ConditionJoin.AND || desc.conditionJoin == ConditionJoin.NONE) && !this."$method"(response))    {
                            isValid = false
                            return true
                        }
                        if (desc.conditionJoin == ConditionJoin.OR && this."$method"(response))    {
                            isValid = true
                            return true
                        }
                    }
                } catch(MissingPropertyException ex)   {
                    log.warn "Attribute ${it.attribute} not found for ${instance}"
                }
            }
        }
        return isValid
    }

    /**
     * Helper method checking if list contains attribute
     * @param attribute
     * @param list
     * @return logical result of operation
     */
    boolean inList(attribute, list) {
        list.contains(attribute)
    }

    /**
     * Helper method checking if attribute is smaller than value
     * @param attribute
     * @param value
     * @return logical result of operation
     */
    boolean lessThan(attribute, value) {
        attribute < value
    }

    /**
     * Helper method checking if attribute is smaller or equal to value
     * @param attribute
     * @param value
     * @return logical result of operation
     */
    boolean lessThanEquals(attribute, value) {
        attribute <= value
    }

    /**
     * Helper method checking if attribute is greater than value
     * @param attribute
     * @param value
     * @return logical result of operation
     */
    boolean greaterThan(attribute, value) {
        attribute > value
    }

    /**
     * Helper method checking if attribute is greater than or equal to value
     * @param attribute
     * @param value
     * @return logical result of operation
     */
    boolean greaterThanEquals(attribute, value) {
        attribute >= value
    }

    /**
     * Helper method checking if attribute matches regex (case sensitive)
     * @param attribute
     * @param regex regular expression
     * @return logical result of operation
     */
    boolean like(attribute, regex)  {
        attribute ==~ regex
    }

    /**
     * Helper method checking if attribute matches regex (case insensitive)
     * @param attribute
     * @param regex regular expression
     * @return logical result of operation
     */
    boolean iLike(attribute, regex)  {
        attribute.toUpperCase() ==~ regex.toUpperCase()
    }

    /**
     * Helper method checking if attribute matches regex (case sensitive)
     * @param attribute
     * @param regex regular expression
     * @return logical result of operation
     */
    boolean rLike(attribute, regex)  {
        attribute ==~ regex
    }

    /**
     * Helper method checking if attribute is not equal value
     * @param attribute
     * @param value
     * @return logical result of operation
     */
    boolean notEqual(attribute, value) {
        attribute != value
    }

    /**
     * Helper method checking if attribute is equal value
     * @param attribute
     * @param value
     * @return logical result of operation
     */
    boolean equals(attribute, value)   {
        attribute == value
    }

    /**
     * Helper method checking if attribute contains value
     * @param attribute
     * @param value
     * @return logical result of operation
     */
    boolean contains(attribute, value)  {
        boolean isCollection = attribute instanceof Collection
        boolean isList = attribute instanceof List
        boolean isSet = attribute instanceof Set
        boolean isArray = attribute != null && attribute.getClass().isArray()
        boolean isMap = attribute instanceof Map
        if(isCollection || isList || isSet || isArray)
            return attribute.contains(value)
        if(isMap)
            return attribute.containsValue(value)
        return attribute == value
    }

    /**
     * Helper method checking if attribute is null
     * @param attribute
     * @return logical result of operation
     */
    boolean isNull(attribute)   {
        attribute == null
    }

    /**
     * Helper method checking if attribute isn't null
     * @param attribute
     * @return logical result of operation
     */
    boolean isNotNull(attribute)   {
        attribute != null
    }

    /**
     * Helper method checking if attribute is in range
     * @param attribute
     * @param range
     * @return logical result of operation
     */
    boolean inRange(attribute, range)   {
        range.contains(attribute)
    }

    /**
     * Helper method checking if attribute is between lowerBound and upperBound
     * @param attribute
     * @param lowerBound
     * @param upperBound
     * @return logical result of operation
     */
    boolean between(attribute, lowerBound, upperBound)  {
        attribute >= lowerBound && attribute <= upperBound
    }

    /**
     * Helper method, converting string in UNDERSCORE_NOTATION to string
     * in camelCase notation
     * @param underscore given string to be converted
     * @return string in camelCase notation
     */
    String underscoreToCamelCase(String underscore){
        if(underscore?.getClass() != String || underscore.isAllWhitespace()){
            return ""
        }
        underscore = underscore.toLowerCase()
        return underscore.replaceAll(/_\w/){ it[1].toUpperCase() }
    }

}
