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
 * This class servers as a parser for GORM API methods - save,
 * delete, get, list and dynamic finders and transforms them
 * into query descriptors.
 */
@Log4j
class GormApiParser implements CallingParser {

    /** List of allowed finder conditions **/
    private static final List<String> CONDITIONS = [
        "InList",
        "LessThanEquals",
        "LessThan",
        "GreaterThanEquals",
        "GreaterThan",
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

    /** List of allowed types of operations **/
    private static final List<String> ALLOWED_OPERATIONS = [
            "create",
            "update",
            "delete"
    ]

    /** List of allowed type of finders **/
    private static final List<String> ALLOWED_FINDERS = [
            "find",
            "findAll",
            "get",
            "list"
    ]

    /**
     * This method parses dynamic finders based on method name into query descriptors.
     * @param clazz name of class method is called on
     * @param finder name of finder
     * @param params method params
     * @return parsed query descriptor
     */
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

    /**
     * This method parses instance - modifying methods save and delete into query descriptors
     * @param operation type of operation
     * @param instance instance method is called on
     * @return parsed query descriptor
     */
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
