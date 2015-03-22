package query.builder

import parsers.config.CachedConfigParser
import query.ConditionJoin
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

            if (!desc.conditions.empty && desc.conditions[0] instanceof SimpleCondition) {
                if (operation["endpoint"].contains("[:${desc.conditions[0].attribute}]")) {
                    tempUrl += operation["endpoint"]
                    tempUrl = tempUrl.replace("[:${desc.conditions[0].attribute}]", "${desc.conditions[0].value}")
                }
                else {
                    tempUrl += operation["queryEndpoint"]?: (operation["endpoint"]).replaceAll(/\/\[\:.*\]/, "") + "?${desc.conditions[0].attribute}=${desc.conditions[0].value}"
                }
            }
        }
        else if(desc.conditionJoin == ConditionJoin.AND || desc.conditions.empty) {
            tempUrl += operation["queryEndpoint"]?: (operation["endpoint"]).replaceAll(/\/\[\:.*\]/, "")
            desc.conditions.eachWithIndex { condition, index ->
                if (condition.comparator == Operator.EQUALS && condition instanceof SimpleCondition) {
                    if (index == 0)
                        tempUrl += "?"
                    else
                        tempUrl += "&"
                    tempUrl += "${condition.attribute}=${condition.value}"
                }
            }
        }
        new RestRemoteQuery(method: operation["method"], url: tempUrl)
    }

    private void generateReadQuery() {

    }

    private void generateCreateQuery() {

    }

    private void generateUpdateQuery() {

    }

    private void generateDeleteQuery() {

    }
}
