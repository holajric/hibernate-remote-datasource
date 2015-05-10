package groovy.org.grails.datastore.remote.hibernate.query

/**
 * Class describing executed query and containing all informations
 * necessary to rebuild it for remote sdata source.
 */
class QueryDescriptor {
    /** Name of entity that is queried**/
    String entityName
    /** List of limiting conditions **/
    List<Condition> conditions
    /** Type of query **/
    Operation operation
    /** Relation between conditions **/
    ConditionJoin conditionJoin
    /** Map containing parameters defining paginating and sorting of query result **/
    def paginationSorting

    /**
     * Constructor initialises conditions and paginationSorting as empty collections,
     * and sets default conditionJoin as NONE
     */
    QueryDescriptor()   {
        this.conditions = new ArrayList<Condition>()
        this.paginationSorting = [:]
        this.conditionJoin = ConditionJoin.NONE
    }

    /**
     * Simple toString method used only for loggin purposes
     * @return string representation of object
     */
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
