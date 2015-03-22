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

    static simpleConditions = [
        "InList",
        "LessThan",
        "LessThanEquals",
        "GreaterThan",
        "GreaterThanEquals",
        "Like",
        "Ilike",
        "NotEqual",
        "InRange",
        "Rlike"
    ]

    QueryDescriptor parseFinder(String clazz, String finder, params)   {
        println finder
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
                if (subquery.contains("Between")) {
                    subquery = subquery.replace("Between", "")
                    queryDesc.conditions.add(new IntervalCondition(attribute: subquery, comparator: Operator.BETWEEN, lowerBound: params[counter], upperBound: params[counter + 1]))
                    counter += 2
                } else if (subquery.contains("IsNotNull")) {
                    subquery = subquery.replace("IsNotNull", "")
                    queryDesc.conditions.add(new Condition(attribute: subquery, comparator: Operator.IS_NOT_NULL))
                } else if (subquery.contains("IsNull")) {
                    subquery = subquery.replace("IsNull", "")
                    queryDesc.conditions.add(new Condition(attribute: subquery, comparator: Operator.IS_NULL))
                } else {
                    Operator operator = Operator.EQUALS
                    simpleConditions.eachWithIndex { simpleCondition, i ->
                        if (subquery.contains(simpleCondition)) {
                            subquery = subquery.replace(simpleCondition, "")
                            operator = Operator.values()[i]
                        }
                    }
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
                queryDesc.paginationSorting["limit"] = 1
        println queryDesc
        return queryDesc
    }

    QueryDescriptor parseInstanceMethod(String operation, instance) {
        def queryDesc = new QueryDescriptor(entityName: instance.class.getName())
        switch(operation)   {
            case "save":
                queryDesc.operation = Operation.CREATE
                break;
            case "update":
                queryDesc.operation = Operation.UPDATE
                queryDesc.conditions.add(new SimpleCondition(attribute: "id", comparator: Operator.EQUALS, value: instance.id ))
                break;
            case "delete":
                queryDesc.operation = Operation.DELETE
                queryDesc.conditions.add(new SimpleCondition(attribute: "id", comparator: Operator.EQUALS, value: instance.id ))
                break;
        }

        queryDesc
    }
}
