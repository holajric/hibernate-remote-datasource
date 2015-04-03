package query.builder

import groovy.util.logging.Log4j
import parsers.config.CachedConfigParser
import query.ConditionJoin
import query.IntervalCondition
import query.Operation
import query.Operator
import query.QueryDescriptor
import query.SimpleCondition
import query.Condition
import query.builder.formatters.Formatter

/**
 * Created by richard on 18.2.15.
 */
@Log4j
class RestQueryBuilder implements QueryBuilder {
    RestRemoteQuery generateQuery(QueryDescriptor desc) {
        if(!dec.entityName || desc.entityName.empty)   {
            log.info "Descriptor entityName is required"
            return null
        }
        String tempUrl = CachedConfigParser.mapping[desc.entityName]["baseUrl"]
        if(!CachedConfigParser.mapping[desc.entityName]["baseUrl"])   {
            log.info "Base URL is required"
            return null
        }
        def operation
        if((operation = CachedConfigParser.getQueryOperation(desc)) == null) {
            log.info "Operation configuration for ${desc.entityName} ${desc.operation} could not be loaded"
            return null
        }
        if (isSingleQuery(desc, operation["endpoint"])) {
            if(!operation["endpoint"])  {
                log.info "Operation endpoint is required"
                return null
            }
            tempUrl += operation["endpoint"]
            tempUrl = tempUrl.replaceAll(/\[:${desc.conditions[0].attribute}(\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*\]/, "${Formatter.formatAttribute(tempUrl, desc.conditions[0].attribute, desc.conditions[0].value)}")
        }   else    {
            tempUrl+= generateBatchQuery(desc, operation)
        }
        println "${desc.operation} ${operation["method"]} ${tempUrl}" //TODO: delete
        if(!operation["method"])    {
            log.info "Operation method is required"
            return null
        }
        return new RestRemoteQuery(method: operation["method"], url: tempUrl)
    }

    boolean isSingleQuery(QueryDescriptor desc, String endpoint)  {
        return ((desc.conditionJoin == ConditionJoin.NONE) &&
                (!desc.conditions.empty) &&
                (desc.conditions[0] instanceof SimpleCondition) &&
                (desc.conditions[0].comparator == Operator.EQUALS) &&
                (endpoint?.contains("[:${desc.conditions[0].attribute}]")))
    }

    String generateBatchQuery(QueryDescriptor desc, operation)    {
        if(desc.conditionJoin == ConditionJoin.OR && !desc.conditions.empty) {
            log.info "Rest queries do not support Or conditions, they will be skipped and filtered locally"
            return ""
        }
        def tempUrl = operation["queryEndpoint"]?: (operation["endpoint"])?.replaceAll(/\/\[\:.*\]/, "") ?: {
            log.info "Operation ${desc.operation} for ${desc.entityName} has no endpoint, empty endpoint will be used"
            return ""
        }
        boolean first = true
        desc.conditions.eachWithIndex { condition, index ->
            if(!isConditionSupported(condition, desc)) {
                log.info "condition $condition is not supported by this QueryBuilder, it will be skipped and filtered locally"
            }   else {
                String conditionString = generateConditionQuery(condition, desc)
                if (conditionString.empty) {
                    log.info "condition $condition is not supported by this QueryBuilder, it will be skipped and filtered locally"
                }   else {
                    tempUrl += first ? "?" : "&"
                    first = false
                    tempUrl += conditionString
                }
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

    String generateConditionQuery(Condition condition, QueryDescriptor desc)  {
        String tempUrl = ""
        if (condition.comparator == Operator.EQUALS && condition instanceof SimpleCondition) {
            tempUrl = "${condition.attribute}=${condition.value}"
        }
        if (CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}") {
            tempUrl = CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}"
            if (condition instanceof SimpleCondition)
                tempUrl = tempUrl.replaceAll(/\[:value(\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*\]/, "${Formatter.formatAttribute(tempUrl, "value", condition.value)}")
            if (condition instanceof IntervalCondition)
                tempUrl = tempUrl.replaceAll(/\[:lowerBound(\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*\]/, "${Formatter.formatAttribute(tempUrl, "lowerBound", condition.lowerBound)}").replaceAll(/\[:upperBound\](\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*/, "${Formatter.formatAttribute(tempUrl, "upperBound", condition.upperBound)}")
        }
        return tempUrl
    }

    boolean isConditionSupported(Condition condition, QueryDescriptor desc)  {
        if(condition.attribute.empty)  {
            log.info "Condition attribute is required"
            return false
        }
        if(condition instanceof SimpleCondition && !condition.value)  {
            log.info "Condition value is required for SimpleCondition"
            return false
        }
        if(condition instanceof IntervalCondition && (!condition.lowerBound || !condition.upperBound))  {
            log.info "Condition lowerBound and upperBound are required for IntervalCondition"
            return false
        }
        //TODO SPLIT CONDITION INTO MORE WITH LOGGING
        return (
        (condition.comparator == Operator.EQUALS && condition instanceof SimpleCondition) ||
        (CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}")
        )
    }




}
