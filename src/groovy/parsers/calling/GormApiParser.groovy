package parsers.calling

import query.ConditionJoin
import query.IntervalCondition
import query.Operation
import query.Operator
import query.QueryDescriptor
import query.Condition
import query.SimpleCondition
import java.beans.Introspector

/**
 * Created by richard on 18.2.15.
 */
class GormApiParser implements CallingParser {

    static conditions = [
        "InList",
        "LessThan",
        "LessThanEquals",
        "GreaterThan",
        "GreaterThanEquals",
        "Like",
        "Ilike",
        "NotEqual",
        "InRange",
        "Rlike",
        "Between",
        "IsNotNull",
        "IsNull"
    ]

    QueryDescriptor parseFinder(String clazz, String finder, params)   {

        def splitted = finder.replaceFirst(/By/,"<SPLIT>").split(/<SPLIT>/)
        String operation = splitted?.getAt(0)
        String query
        try {
            query = splitted?.getAt(1)
        } catch(ArrayIndexOutOfBoundsException e) {
            query = null
        }
        ConditionJoin conditionJoin = ConditionJoin.NONE
        def subqueries = [query]
        if(query) {
            if ((subqueries = query.split(/And/)).size() > 1) {
                conditionJoin = ConditionJoin.AND
            } else if ((subqueries = query.split(/Or/)).size() > 1) {
                conditionJoin = ConditionJoin.OR
            }
        }
        def queryDesc = new QueryDescriptor(entityName: clazz, conditionJoin: conditionJoin, operation: Operation.READ)

        def counter = 0
        if(query) {
            subqueries.each { subquery ->
                subquery = Introspector.decapitalize(subquery)
                Operator operator = Operator.EQUALS
                conditions.eachWithIndex { condition, i ->
                    if (subquery.contains(condition)) {
                        subquery = subquery.replace(condition, "")
                        operator = Operator.values()[i]
                    }
                }
                if (operator == Operator.BETWEEN) {
                    queryDesc.conditions.add(new IntervalCondition(attribute: subquery, comparator: operator, lowerBound: params[counter], upperBound: params[counter + 1]))
                    counter += 2
                } else if(operator == Operator.IS_NOT_NULL) {
                    queryDesc.conditions.add(new Condition(attribute: subquery, comparator: operator))
                } else if (operator == Operator.IS_NULL) {
                    queryDesc.conditions.add(new Condition(attribute: subquery, comparator: operator))
                } else {
                    queryDesc.conditions.add(new SimpleCondition(attribute: subquery, comparator: operator, value: params[counter]))
                    counter++
                }
            }
        }
        if(params.size() == counter + 1)   {
            queryDesc.paginationSorting = params[counter]
        }
        if(operation != "find" && operation != "findAll")
            return null
        queryDesc.operation = Operation.READ

        if(operation == "find")
                queryDesc.paginationSorting["max"] = 1

        return queryDesc
    }

    QueryDescriptor parseInstanceMethod(String operation, instance) {
        def queryDesc = new QueryDescriptor(entityName: instance.class.getName())
        queryDesc.operation = Operation."${operation.toUpperCase()}"
        if(operation == "update" || operation == "delete")   {
            queryDesc.conditions.add(new SimpleCondition(attribute: "id", comparator: Operator.EQUALS, value: instance.id ))
        }

        queryDesc
    }
}
