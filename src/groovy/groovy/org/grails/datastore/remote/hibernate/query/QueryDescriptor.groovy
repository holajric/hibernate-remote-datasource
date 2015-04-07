package groovy.org.grails.datastore.remote.hibernate.query

/**
 * Created by richard on 18.2.15.
 */
class QueryDescriptor {
    String entityName
    List<Condition> conditions
    Operation operation
    ConditionJoin conditionJoin
    def paginationSorting

    QueryDescriptor()   {
        this.conditions = new ArrayList<Condition>()
        this.paginationSorting = [:]
        this.conditionJoin = ConditionJoin.NONE
    }

    String toString()   {
        String temp = operation.toString() + " " + entityName + "\n"
        if(conditions.size() != 0) {
            temp += conditionJoin.toString() + " { \n"
            conditions.each{ condition ->
                temp += "\t"+ condition.toString() + "\n"
            }
            temp += "}\n"
        }

        paginationSorting.each  { option ->
            temp += option.key.toString().toUpperCase() + " "+ option.value.toString() + "\n"
        }

        temp
    }
}
