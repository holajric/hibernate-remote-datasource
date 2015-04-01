package parsers.config

import connectors.DataSourceConnector
import grails.transaction.Transactional
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import query.Operation
import query.QueryDescriptor
import query.builder.QueryBuilder
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import auth.Authenticator

@Transactional
class CachedConfigParser {
    static Map<String, DataSourceConnector> dataSourceConnector = new HashMap<String, DataSourceConnector>()
    static Map<String, QueryBuilder> queryBuilder = new HashMap<String, QueryBuilder>()
    static Map<String, Authenticator> authenticator = new HashMap<String, Authenticator>()
    static Map<String, Object> mapping  = new HashMap<String, Object>()
    static Map<String, Map<String, String>> attributeMapping  = new HashMap<String, Map<String, String>>()
    static Map<String, Map<String, Object>> authenticationParams  = new HashMap<String, Map<String, Object>>()

    static boolean isRemote(Class entity)  {
        if(!mapping[entity.getName()])
            mapping[entity.getName()] = GrailsClassUtils.getStaticPropertyValue(entity,'remoteMapping')
        mapping[entity.getName()] != null
    }

    static DataSourceConnector getDataSourceConnector(QueryDescriptor desc)    {
        if(!isRemote(Class.forName(desc.entityName)))
            return null
        if(!dataSourceConnector[desc.entityName])
            dataSourceConnector[desc.entityName] = Class.forName("connectors."+mapping[desc.entityName]["sourceType"]+"DataSourceConnector")?.newInstance()
        dataSourceConnector[desc.entityName]
    }

    static QueryBuilder getQueryBuilder(QueryDescriptor desc)  {
        if(!isRemote(Class.forName(desc.entityName)))
            return null
        if(!queryBuilder[desc.entityName])
            queryBuilder[desc.entityName] = Class.forName("query.builder."+mapping[desc.entityName]["sourceType"]+"QueryBuilder")?.newInstance()
        queryBuilder[desc.entityName]
    }

    static Authenticator getAuthenticator(QueryDescriptor desc) {
        if(!isRemote(Class.forName(desc.entityName)))
            return null
        if(!authenticator["${desc.entityName} ${desc.operation}}"])  {
            String name = (mapping?."$desc.entityName"?."operations"?.getAt(desc.operation)?."authentication"
                          ?: mapping[desc.entityName]["authentication"]
                          ?: "Token")
            authenticator["${desc.entityName} ${desc.operation}}"] = Class.forName("auth.${name}Authenticator")?.newInstance(desc.entityName, desc.operation)
        }

        return authenticator["${desc.entityName} ${desc.operation}}"]
    }

    static Map<String, Object> getAuthenticationParams(String entity, Operation operation) {
        if(!isRemote(Class.forName(entity)))
            return null
        if(!authenticationParams["${entity} ${operation}}"])  {
            authenticationParams["${entity} ${operation}}"] = (mapping?."$entity"?."operations"?.getAt(operation)?."authenticationParams"
                    ?: mapping[entity]["authenticationParams"]
                    ?: [:])
        }

        return authenticationParams["${entity} ${operation}}"]
    }

    static boolean isOperationAllowed(QueryDescriptor desc) {
        if(!isRemote(Class.forName(desc.entityName)))
            return null
        mapping[desc.entityName]["allowed"] == null || mapping[desc.entityName]["allowed"].contains(desc.operation)
    }

    static Map<String, String> getAttributeMap(QueryDescriptor desc)   {
        if(!isRemote(Class.forName(desc.entityName)))
            return null
        if(!attributeMapping[desc.entityName]) {
            attributeMapping[desc.entityName] = [:]
            new DefaultGrailsDomainClass(Class.forName(desc.entityName)).properties.each { it ->
                if(it.name != "version" && !mapping?."$desc.entityName"?."local"?.contains(it.name))
                    attributeMapping?."$desc.entityName"?."${it.name}" = mapping?."$desc.entityName"?."mapping"?."${it.name}" ?: it.name
            }
        }
        attributeMapping[desc.entityName]
    }

    static Map<String, Object> getQueryOperation(QueryDescriptor desc)   {
        if(!isOperationAllowed(desc))
            return null
        if(!mapping?."$desc.entityName"?."operations"?.getAt(desc.operation))
            mapping?."$desc.entityName"?."operations"?.putAt(desc.operation, [:]) //THERE WILL BE DEFAULT
        mapping?."$desc.entityName"?."operations"?.getAt(desc.operation)
    }
}
