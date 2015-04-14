package groovy.org.grails.datastore.remote.hibernate.parsers.calling

import groovy.util.logging.Log4j
import groovy.org.grails.datastore.remote.hibernate.query.ConditionJoin
import groovy.org.grails.datastore.remote.hibernate.query.IntervalCondition
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import groovy.org.grails.datastore.remote.hibernate.query.Operator
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor
import groovy.org.grails.datastore.remote.hibernate.query.Condition
import groovy.org.grails.datastore.remote.hibernate.query.SimpleCondition
import java.beans.Introspector

/**
 * Created by richard on 18.2.15.
 */
@Log4j
class GormApiParser implements CallingParser {

    private static final List<String> CONDITIONS = [
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
        "IsNull",
        "Contains"
    ]

    private static final List<String> ALLOWED_OPERATIONS = [
            "create",
            "update",
            "delete"
    ]

    private static final List<String> ALLOWED_FINDERS = [
            "find",
            "findAll",
            "get",
            "list"
    ]

    QueryDescriptor parseFinder(String clazz, String finder, params)   {
        if(!(finder instanceof String) || !(clazz instanceof String)) {
            log.error "Finder and className have to be strings"
            return null
        }
        def splitted = finder.replaceFirst(/By/,"<SPLIT>").split(/<SPLIT>/)
        String operation = splitted?.getAt(0)
        if(!ALLOWED_FINDERS.contains(operation)) {
            log.error "finder $operation is not supported"
            return null
        }
        String query = (splitted.size() > 1) ? splitted?.getAt(1) : null

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
                CONDITIONS.eachWithIndex { condition, i ->
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

        if(params?.size() >= counter + 1)   {
            queryDesc.paginationSorting = params[counter]
        }

        if(operation == "find")
            queryDesc.paginationSorting["max"] = 1

        return queryDesc
    }

    QueryDescriptor parseInstanceMethod(String operation, instance) {
        if(instance == null)    {
            log.error "Instance is required"
            return null
        }
        if(!ALLOWED_OPERATIONS.contains(operation)) {
            log.error "Operation $operation is not supported"
            return null
        }
        def queryDesc = new QueryDescriptor(entityName: instance.class.getName())
        queryDesc.operation = Operation."${operation.toUpperCase()}"
        if(operation == "update" || operation == "delete")   {
            if(instance?.id)
                queryDesc.conditions.add(new SimpleCondition(attribute: "id", comparator: Operator.EQUALS, value: instance.id ))
        }

        return queryDesc
    }
}
