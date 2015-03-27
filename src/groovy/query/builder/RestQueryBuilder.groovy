package query.builder

import parsers.config.CachedConfigParser
import query.ConditionJoin
import query.IntervalCondition
import query.Operation
import query.Operator
import query.QueryDescriptor
import query.SimpleCondition
import query.Condition

/**
 * Created by richard on 18.2.15.
 */
class RestQueryBuilder implements QueryBuilder {
    RestRemoteQuery generateQuery(QueryDescriptor desc) {

        def url = CachedConfigParser.mapping[desc.entityName]["baseUrl"]
        def operation = CachedConfigParser.getQueryOperation(desc)

        String tempUrl = url
        if(desc.operation == Operation.CREATE)  {
            return new RestRemoteQuery(method: operation["method"], url: tempUrl + operation["endpoint"])
        }

        if(desc.conditionJoin == ConditionJoin.NONE && !desc.conditions.empty && !CachedConfigParser.mapping[desc.entityName]?."local"?.contains(desc.conditions[0].attribute)) {
            if (desc.conditions[0].comparator == Operator.EQUALS && desc.conditions[0] instanceof SimpleCondition && operation["endpoint"].contains("[:${desc.conditions[0].attribute}]")) {
                tempUrl += operation["endpoint"]
                tempUrl = tempUrl.replace("[:${desc.conditions[0].attribute}]", "${desc.conditions[0].value}")
            } else {
                tempUrl += operation["queryEndpoint"] ?: (operation["endpoint"]).replaceAll(/\/\[\:.*\]/, "")
                if(isConditionSupported(desc.conditions[0], desc)) {
                    tempUrl+="?"
                    tempUrl+= generateConditionQuery(desc.conditions[0], desc)
                }
            }
        }   else if(desc.conditionJoin == ConditionJoin.AND || desc.conditions.empty) {
            tempUrl += operation["queryEndpoint"]?: (operation["endpoint"]).replaceAll(/\/\[\:.*\]/, "")
            boolean first = true
            desc.conditions.eachWithIndex { condition, index ->
                if(isConditionSupported(condition, desc))   {
                    tempUrl += first ? "?" : "&"
                    first = false
                    tempUrl += generateConditionQuery(condition, desc)
                }
            }
        }
        new RestRemoteQuery(method: operation["method"], url: tempUrl)
    }

    String generateConditionQuery(Condition condition, QueryDescriptor desc)  {
        String tempUrl = ""

        if (condition.comparator == Operator.EQUALS && condition instanceof SimpleCondition) {
            tempUrl += "${condition.attribute}=${condition.value}"
        }

        if (CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}") {
            tempUrl += CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}"
            if (condition instanceof SimpleCondition)
                tempUrl = tempUrl.replaceAll(":value", "${condition.value}")
            if (condition instanceof IntervalCondition)
                tempUrl = tempUrl.replaceAll(":lowerBound", "${condition.lowerBound}").replaceAll(":upperBound", "${condition.upperBound}")
        }

        return tempUrl
    }

    boolean isConditionSupported(Condition condition, QueryDescriptor desc)  {
        return (
        (!CachedConfigParser.mapping[desc.entityName]?."local"?.contains(condition.attribute)) ||
        (condition.comparator == Operator.EQUALS && condition instanceof SimpleCondition) ||
        (CachedConfigParser.mapping[desc.entityName]?."queryMapping"?."${condition.conditionString()}")
        )
    }


}
