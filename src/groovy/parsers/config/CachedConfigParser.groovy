package parsers.config

import connectors.DataSourceConnector
import grails.transaction.Transactional
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import query.QueryDescriptor
import query.builder.QueryBuilder
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass

@Transactional
class CachedConfigParser {
    static Map<String, DataSourceConnector> dataSourceConnector = new HashMap<String, DataSourceConnector>()
    static Map<String, QueryBuilder> queryBuilder = new HashMap<String, QueryBuilder>()
    static Map<String, Object> mapping  = new HashMap<String, Object>()
    static Map<String, Object> attributeMapping  = new HashMap<String, Map<String, String>>()

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
                def currentMapping = mapping?."$desc.entityName"?."mapping"?."${it.name}"
                if(it.name != "version")
                    attributeMapping?."$desc.entityName"?."${it.name}" = currentMapping?."remote_name"?:(currentMapping?:it.name)
            }
        }
        attributeMapping[desc.entityName]
    }

    static Map<String, Object> getAttributeOperation(QueryDescriptor desc, String attribute)   {
        if(!isRemote(Class.forName(desc.entityName)))
            return null
        if(!mapping?."$desc.entityName"?."mapping"?."operations"?.getAt(desc.operation)) {
            if(!mapping?."$desc.entityName"?."operations"?.getAt(desc.operation)) {
                mapping?."$desc.entityName"?."mapping"?."operations"?.putAt(desc.operation, [:]) //THERE WILL BE DEFAULT
            }
            mapping?."$desc.entityName""mapping"?."operations"?.putAt(desc.operation, mapping?."$desc.entityName"?."operations"?.getAt(desc.operation))
        }
        mapping[desc.entityName]["$attribute"]["operations"][desc.operation]
    }

    static Map<String, Map<String, Object>> getQueryOperations(QueryDescriptor desc)   {
        if(!isRemote(Class.forName(desc.entityName)))
            return null
        Map<String, Map<String, Object>> attributesOperations = new HashMap<String, Map<String,Object>>()
        new DefaultGrailsDomainClass(Class.forName(desc.entityName)).properties.each { it ->
            if(it.name != "version") {
                if (mapping?."$desc.entityName"?."mapping"?."$it.name" instanceof String || !mapping?."$desc.entityName"?."mapping"?."$it.name"?."operations"?.getAt(desc.operation)) {
                    if (!mapping?."$desc.entityName"?."mapping"?."$it.name" || mapping?."$desc.entityName"?."mapping"?."$it.name" instanceof String)
                        mapping?."$desc.entityName"?."mapping"?."$it.name" = ["remote_name": mapping?."$desc.entityName"?."mapping"?."$it.name" ?: "$it.name", "operations": [:]]
                    if(!mapping?."$desc.entityName"?."mapping"?."$it.name"?."operations")
                        mapping?."$desc.entityName"?."mapping"?."$it.name"?."operations" = [:]
                    if (!mapping?."$desc.entityName"?."operations"?.getAt(desc.operation))
                        mapping?."$desc.entityName"?."mapping"?."$it.name"?."operations"?.putAt(desc.operation, [:]) //THERE WILL BE DEFAULT
                    mapping?."$desc.entityName"?."mapping"?."$it.name"?."operations"?.putAt(desc.operation, mapping?."$desc.entityName"?."operations"?.getAt(desc.operation))
                }
                attributesOperations?.putAt("$it.name", mapping?."$desc.entityName"?."mapping"?."$it.name"?."operations"?.getAt(desc.operation))
            }
        }

        attributesOperations
    }
}
