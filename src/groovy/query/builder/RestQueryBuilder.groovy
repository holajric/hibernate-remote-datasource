package query.builder

import parsers.config.CachedConfigParser
import query.ConditionJoin
import query.IntervalCondition
import query.Operator
import query.QueryDescriptor
import query.SimpleCondition

/**
 * Created by richard on 18.2.15.
 */
class RestQueryBuilder implements QueryBuilder {
    RestRemoteQuery generateQuery(QueryDescriptor desc) {
        def url = CachedConfigParser.mapping[desc.entityName]["baseUrl"]
        def operation = CachedConfigParser.getQueryOperation(desc)
        String tempUrl = url
        if(desc.conditionJoin == ConditionJoin.NONE ) {
            if (!desc.conditions.empty) {
                if (!CachedConfigParser.mapping[desc.entityName]?."local"?.contains(desc.conditions[0].attribute)) {
                    if (desc.conditions[0].comparator == Operator.EQUALS && desc.conditions[0] instanceof SimpleCondition && operation["endpoint"].contains("[:${desc.conditions[0].attribute}]")) {
                        tempUrl += operation["endpoint"]
                        tempUrl = tempUrl.replace("[:${desc.conditions[0].attribute}]", "${desc.conditions[0].value}")
                    } else {
                        tempUrl += operation["queryEndpoint"] ?: (operation["endpoint"]).replaceAll(/\/\[\:.*\]/, "")
                        if (desc.conditions[0].comparator == Operator.EQUALS && desc.conditions[0] instanceof SimpleCondition) {
                            tempUrl += "?${desc.conditions[0].attribute}=${desc.conditions[0].value}"
                        }
                        if (CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${desc.conditions[0].conditionString()}") {
                            println tempUrl
                            tempUrl += "?" + CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${desc.conditions[0].conditionString()}"
                            println tempUrl
                            if (desc.conditions[0] instanceof SimpleCondition)
                                tempUrl = tempUrl.replaceAll(":value", "${desc.conditions[0].value}")
                            if (desc.conditions[0] instanceof IntervalCondition)
                                tempUrl = tempUrl.replaceAll(":lowerBound", "${desc.conditions[0].lowerBound}").replaceAll(":upperBound", "${desc.conditions[0].upperBound}")
                        }
                    }
                }
            }
        }
        else if(desc.conditionJoin == ConditionJoin.AND || desc.conditions.empty) {
            tempUrl += operation["queryEndpoint"]?: (operation["endpoint"]).replaceAll(/\/\[\:.*\]/, "")
            boolean first = true
            desc.conditions.eachWithIndex { condition, index ->
                if (!CachedConfigParser.mapping[desc.entityName]?."local"?.contains(condition.attribute)) {
                    if (condition.comparator == Operator.EQUALS && condition instanceof SimpleCondition) {
                        if (first) {
                            tempUrl += "?"
                            first = false
                        } else
                            tempUrl += "&"
                        tempUrl += "${condition.attribute}=${condition.value}"
                    }
                    if (CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}") {
                        if (first) {
                            tempUrl += "?"
                            first = false
                        } else
                            tempUrl += "&"
                        tempUrl += CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}"
                        if (condition instanceof SimpleCondition)
                            tempUrl = tempUrl.replaceAll(":value", "${condition.value}")
                        if (condition instanceof IntervalCondition)
                            tempUrl = tempUrl.replaceAll(":lowerBound", "${condition.lowerBound}").replaceAll(":upperBound", "${condition.upperBound}")
                    }
                }
            }
        }
        println tempUrl
        new RestRemoteQuery(method: operation["method"], url: tempUrl)
    }
}
