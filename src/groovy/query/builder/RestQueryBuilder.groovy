package query.builder

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
class RestQueryBuilder implements QueryBuilder {
    RestRemoteQuery generateQuery(QueryDescriptor desc) {
        String tempUrl = CachedConfigParser.mapping[desc.entityName]["baseUrl"]
        def operation = CachedConfigParser.getQueryOperation(desc)
        if (isSingleQuery(desc, operation["endpoint"])) {
            tempUrl += operation["endpoint"]
            tempUrl = tempUrl.replaceAll(/\[:${desc.conditions[0].attribute}(\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*\]/, "${Formatter.formatAttribute(tempUrl,desc.conditions[0].attribute, desc.conditions[0].value)}")

        }   else    {
            tempUrl+= generateBatchQuery(desc, operation)
        }
        println "${desc.operation} ${operation["method"]} ${tempUrl}"
        return new RestRemoteQuery(method: operation["method"], url: tempUrl)
    }

    boolean isSingleQuery(QueryDescriptor desc, String endpoint)  {
        return ((desc.conditionJoin == ConditionJoin.NONE) &&
                (!desc.conditions.empty) &&
                (desc.conditions[0].comparator == Operator.EQUALS) &&
                (desc.conditions[0] instanceof SimpleCondition) &&
                (endpoint?.contains("[:${desc.conditions[0].attribute}]")))
    }

    String generateBatchQuery(QueryDescriptor desc, operation)    {
        if(desc.conditionJoin == ConditionJoin.OR && !desc.conditions.empty)
            return ""
        def tempUrl = operation["queryEndpoint"]?: (operation["endpoint"]).replaceAll(/\/\[\:.*\]/, "")
        boolean first = true
        desc.conditions.eachWithIndex { condition, index ->
            if(isConditionSupported(condition, desc))   {
                tempUrl += first ? "?" : "&"
                first = false
                tempUrl += generateConditionQuery(condition, desc)
            }
        }

        desc.paginationSorting.each { param ->
            if(!CachedConfigParser.mapping[desc.entityName]?."supportedParams" ||
                CachedConfigParser.mapping[desc.entityName]?."supportedParams"?.contains(param.key))   {
                tempUrl += first ? "?" : "&"
                first = false
                tempUrl+= ((CachedConfigParser.mapping[desc.entityName]?."paramMapping"?."${param.key}")?:param.key) + "=${param.value}"
            }
        }
        return tempUrl
    }

    String generateConditionQuery(Condition condition, QueryDescriptor desc)  {
        String tempUrl = ""
        if (condition.comparator == Operator.EQUALS && condition instanceof SimpleCondition) {
            tempUrl += "${condition.attribute}=${condition.value}"
        }
        if (CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}") {
            tempUrl += CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}"
            if (condition instanceof SimpleCondition)
                tempUrl = tempUrl.replaceAll(/\[:value(\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*\]/, "${Formatter.formatAttribute(tempUrl,"value", condition.value)}")
            if (condition instanceof IntervalCondition)
                tempUrl = tempUrl.replaceAll(/\[:lowerBound(\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*\]/, "${Formatter.formatAttribute(tempUrl,"lowerBound", condition.lowerBound)}").replaceAll(/\[:upperBound\](\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*/, "${Formatter.formatAttribute(tempUrl,"upperBound", condition.upperBound)}")
        }
        return tempUrl
    }

    boolean isConditionSupported(Condition condition, QueryDescriptor desc)  {
        return (
        (condition.comparator == Operator.EQUALS && condition instanceof SimpleCondition) ||
        (CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}")
        )
    }




}
