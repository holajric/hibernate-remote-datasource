package groovy.org.grails.datastore.remote.hibernate.query.builder

import groovy.util.logging.Log4j
import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
import groovy.org.grails.datastore.remote.hibernate.query.ConditionJoin
import groovy.org.grails.datastore.remote.hibernate.query.IntervalCondition
import groovy.org.grails.datastore.remote.hibernate.query.Operator
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor
import groovy.org.grails.datastore.remote.hibernate.query.SimpleCondition
import groovy.org.grails.datastore.remote.hibernate.query.Condition
import groovy.org.grails.datastore.remote.hibernate.query.builder.formatters.Formatter

/**
 * Class that builds queries for REST remote sources.
 */
@Log4j
class RestQueryBuilder implements QueryBuilder {
    RestRemoteQuery generateHashQuery(QueryDescriptor desc) {
        return generateQuery(desc, "hash")
    }

    /**
     * Generates instance of RestRemoteQuery for given QueryDescriptor
     * @params desc query descriptor
     * @params prefix prefix for loading special endpoint for operations like
     * authentication or data hash (optional)
     * @return generated RestRemoteQuery for query
     */
    RestRemoteQuery generateQuery(QueryDescriptor desc, String prefix = "") {
        if(!desc.entityName || desc.entityName.empty)   {
            log.error "Descriptor entityName is required"
            return null
        }
        String tempUrl
        if((tempUrl = CachedConfigParser.mapping[desc.entityName]["baseUrl"]) == null)   {
            log.error "Base URL is required"
            return null
        }
        def operation
        if((operation = CachedConfigParser.getQueryOperation(desc)) == null) {
            log.error "Operation configuration for ${desc.entityName} ${desc.operation} could not be loaded"
            return null
        }
        String endpoint = prefix.empty ? "endpoint" : prefix + "endpoint".capitalize()
        String queryEndpoint = prefix.empty ? "queryEndpoint" : prefix + "queryEndpoint".capitalize()
        desc.conditions.each    {
            if(operation["endpoint ${it.conditionString()}"])   {
                operation[queryEndpoint] = operation["endpoint ${it.conditionString()}"]
                operation[endpoint] = operation["endpoint ${it.conditionString()}"]
                if (it instanceof SimpleCondition) {
                    operation[queryEndpoint] = operation[queryEndpoint].replaceAll(/\[:value(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(operation[queryEndpoint], "value", it.value)}")
                    operation[endpoint] = operation[endpoint].replaceAll(/\[:value(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(operation[endpoint], "value", it.value)}")
                }
                if (it instanceof IntervalCondition) {
                    operation[queryEndpoint] = operation[queryEndpoint].replaceAll(/\[:lowerBound(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(operation[queryEndpoint], "lowerBound", it.lowerBound)}").replaceAll(/\[:upperBound\](\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*/, "${Formatter.formatAttribute(operation[queryEndpoint], "upperBound", it.upperBound)}")
                    operation[endpoint] = operation[endpoint].replaceAll(/\[:lowerBound(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(operation[endpoint], "lowerBound", it.lowerBound)}").replaceAll(/\[:upperBound\](\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*/, "${Formatter.formatAttribute(operation[endpoint], "upperBound", it.upperBound)}")
                }
            }
        }

        if(!operation[endpoint])  {
            log.error "Operation $prefix endpoint is required"
            return null
        }
        if(!operation["method"])    {
            log.error "Operation method is required"
            return null
        }
        if (isSingleQuery(desc, operation[endpoint])) {
            tempUrl += operation[endpoint]
            tempUrl = tempUrl.replaceAll(/\[:${desc.conditions[0].attribute}(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(tempUrl, desc.conditions[0].attribute, desc.conditions[0].value)}")
        }   else    {
            tempUrl+= generateBatchQuery(desc, operation, prefix)
        }
        return new RestRemoteQuery(method: operation["method"], url: tempUrl)
    }

    /**
     * This method checks if query is for single object or for collection
     * @param desc query descriptor
     * @param endpoint endpoint for query loaded from configuration
     * @return if is single object or collection query
     */
    boolean isSingleQuery(QueryDescriptor desc, String endpoint)  {
        return ((desc.conditionJoin == ConditionJoin.NONE) &&
                (!desc.conditions.empty) &&
                (desc.conditions[0] instanceof SimpleCondition) &&
                (desc.conditions[0].comparator == Operator.EQUALS) &&
                (endpoint?.contains("[:${desc.conditions[0].attribute}]")))
    }
/**
 * This method generates URL (except base URL part, that is given)
 * for remote source for query, that is on collection of data.
 * @param desc query descriptor
 * @param operation map containing configuration for operation
 * @param prefix prefix for loading special endpoint for operations like
 * authentication or data hash (optional)
 * @return part of URL after base URL for query
 */
    String generateBatchQuery(QueryDescriptor desc, operation, prefix = "")    {
        if(desc.conditionJoin == ConditionJoin.OR && !desc.conditions.empty) {
            log.info "Rest queries do not support Or conditions, they will be skipped and filtered locally"
            return ""
        }
        String endpoint = prefix.empty ? "endpoint" : prefix + "endpoint".capitalize()
        String queryEndpoint = prefix.empty ? "queryEndpoint" : prefix + "queryEndpoint".capitalize()
        String tempUrl = operation[queryEndpoint]?: operation.containsKey(endpoint) ? operation[endpoint].replaceAll(/\/\[\:.*\]/, "") : ""
        if(tempUrl.empty) {
            log.warn "Operation ${desc.operation} for ${desc.entityName} has no endpoint, empty endpoint will be used"
            return tempUrl
        }
        boolean first = !tempUrl.contains("?")
        desc.conditions.eachWithIndex { condition, index ->
            if(!isConditionSupported(condition, desc)) {
                log.info "condition $condition is not supported by this QueryBuilder, it will be skipped and filtered locally"
            }   else {
                String conditionString = generateConditionQuery(condition, desc)
                tempUrl += first ? "?" : "&"
                first = false
                tempUrl += conditionString
            }
        }

        desc.paginationSorting.each { param ->
            if(!CachedConfigParser.mapping[desc.entityName]?."supportedParams" ||
                CachedConfigParser.mapping[desc.entityName]?."supportedParams"?.contains(param.key))   {
                tempUrl += first ? "?" : "&"
                first = false
                tempUrl+= ((CachedConfigParser.mapping[desc.entityName]?."paramMapping"?."${param.key}")?:param.key) + "=${param.value}"
            }   else    {
                log.info "${desc.entityName} does not support ${param} param, it will be skipped and filtered locally"
            }
        }
        return tempUrl
    }

    /**
     * This method generates query params for condition in case it is supported.
     * @param condition given condition
     * @param desc given query descriptor
     * @return query params string if possible otherwise empty string
     */
    String generateConditionQuery(Condition condition, QueryDescriptor desc)  {
        String tempUrl = ""
        if (condition.comparator == Operator.EQUALS && condition instanceof SimpleCondition) {
            tempUrl = "${condition.attribute}=${condition.value}"
        }
        if (CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}") {
            tempUrl = CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}"
            if (condition instanceof SimpleCondition)
                tempUrl = tempUrl.replaceAll(/\[:value(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(tempUrl, "value", condition.value)}")
            if (condition instanceof IntervalCondition)
                tempUrl = tempUrl.replaceAll(/\[:lowerBound(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(tempUrl, "lowerBound", condition.lowerBound)}").replaceAll(/\[:upperBound\](\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*/, "${Formatter.formatAttribute(tempUrl, "upperBound", condition.upperBound)}")
        }
        return tempUrl
    }

    /**
     * Checks if condition is supported by remote data source.
     * @param condition to be checked
     * @param desc query descriptor
     * @return if condition is or isn't supported
     */
    boolean isConditionSupported(Condition condition, QueryDescriptor desc)  {
        if(!condition?.attribute || condition.attribute.empty)  {
            log.error "Condition attribute is required"
            return false
        }
        if(condition instanceof SimpleCondition && !condition.value)  {
            log.error "Condition value is required for SimpleCondition"
            return false
        }
        if(condition instanceof IntervalCondition && (!condition.lowerBound || !condition.upperBound))  {
            log.error "Condition lowerBound and upperBound are required for IntervalCondition"
            return false
        }
        //TODO: OPTIONALLY SPLIT CONDITION INTO MORE WITH LOGGING
        return (
        (condition.comparator == Operator.EQUALS && condition instanceof SimpleCondition) ||
        (CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}")
        )
    }




}
