package groovy.org.grails.datastore.remote.hibernate.parsers.config

import groovy.org.grails.datastore.remote.hibernate.connectors.DataSourceConnector
import grails.transaction.Transactional
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.exceptions.GrailsDomainException
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.context.ApplicationContext
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor
import groovy.org.grails.datastore.remote.hibernate.query.builder.QueryBuilder
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import groovy.org.grails.datastore.remote.hibernate.auth.Authenticator
import groovy.org.grails.datastore.remote.hibernate.query.builder.formatters.Formatter

/**
 * Utility class used for accessing configuration and caching it
 * for higher effectivity.
 */
@Log4j
@Transactional
class CachedConfigParser {
    /** Cached data source connectors indexed by domain name **/
    static Map<String, DataSourceConnector> dataSourceConnector = new HashMap<String, DataSourceConnector>()
    /** Cached query builders indexed by domain name **/
    static Map<String, QueryBuilder> queryBuilder = new HashMap<String, QueryBuilder>()
    /** Cached authenticators indexed by domain name and operation **/
    static Map<String, Authenticator> authenticator = new HashMap<String, Authenticator>()
    /** Cached configuration object indexed by domain name **/
    static Map<String, Object> mapping  = new HashMap<String, Object>()
    /** Cached attribute mapping indexed by domain name **/
    static Map<String, Map<String, String>> attributeMapping  = new HashMap<String, Map<String, String>>()
    /** Cached authentication parameters indexed by domain name and operation **/
    static Map<String, Map<String, Object>> authenticationParams  = new HashMap<String, Map<String, Object>>()

    /**
     * Checks if entity is remote and if it is it loads its configuration.
     * @param entity to check
     * @return true or false
     */
    static boolean isRemote(Class entity)  {
        if(!mapping[entity.getName()]) {
            mapping[entity.getName()] = GrailsClassUtils.getStaticPropertyValue(entity, 'remoteMapping')
        }
        if(mapping[entity.getName()] && !mapping[entity.getName()]["sourceType"])    {
            ApplicationContext context = ServletContextHolder.servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT) as ApplicationContext
            mapping[entity.getName()]["sourceType"] = context.getBean(GrailsApplication).config.grails.plugins.hibernateRemoteDatasource.defaults.sourceType
        }
        if(mapping[entity.getName()] && !mapping[entity.getName()]["queryType"]) {
            ApplicationContext context = ServletContextHolder.servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT) as ApplicationContext
            mapping[entity.getName()]["queryType"] = context.getBean(GrailsApplication).config.grails.plugins.hibernateRemoteDatasource.defaults.queryType
        }
        if(!mapping[entity.getName()])    {
            log.info "Class ${entity.getName()} is not remote or does not have valid remote mapping"
            return false
        }
        return true
    }

    /**
     * Gets DataSourceConnector based on configuration for query
     * @param desc query descriptor
     * @return instance of data source connector
     */
    static DataSourceConnector getDataSourceConnector(QueryDescriptor desc)    {
        if(!isValidDescriptor(desc))
            return null
        if(!mapping[desc.entityName]["sourceType"])   {
            log.error "DataSourceConnector type for class has to be set"
            return null
        }
        if(!dataSourceConnector[desc.entityName])
            try {
                dataSourceConnector[desc.entityName] = Class.forName("groovy.org.grails.datastore.remote.hibernate.connectors."+mapping[desc.entityName]["sourceType"]+"DataSourceConnector")?.newInstance()
            }   catch(ClassNotFoundException ex)    {
                log.error "Class groovy.org.grails.datastore.remote.hibernate.connectors."+mapping[desc.entityName]["sourceType"]+"DataSourceConnector does not exist!"
                return null
            }
        return dataSourceConnector[desc.entityName]
    }

    /**
     * Gets QueryBuilder based on configuration for query
     * @param desc query descriptor
     * @return instance of query builder
     */
    static QueryBuilder getQueryBuilder(QueryDescriptor desc)  {
        if(!isValidDescriptor(desc))
            return null
        if(!mapping[desc.entityName]["queryType"])   {
            log.error "QueryBuilder type for class has to be set"
            return null
        }
        if(!queryBuilder[desc.entityName])
            try {
                queryBuilder[desc.entityName] = Class.forName("groovy.org.grails.datastore.remote.hibernate.query.builder."+mapping[desc.entityName]["queryType"]+"QueryBuilder")?.newInstance()
            }   catch(ClassNotFoundException ex)    {
                log.error "Class groovy.org.grails.datastore.remote.hibernate.query.builder."+mapping[desc.entityName]["queryType"]+"QueryBuilder does not exist!"
                return null
            }
        return queryBuilder[desc.entityName]
    }

    /**
     * Gets Authenticator based on configuration for query
     * @param desc query descriptor
     * @return instance of authenticator
     */
    static Authenticator getAuthenticator(QueryDescriptor desc) {
        if(!isValidDescriptor(desc))
            return null
        if(!authenticator["${desc.entityName} ${desc.operation}"])  {
            String name = (mapping?."$desc.entityName"?."operations"?.getAt(desc.operation)?."authentication"
                          ?: mapping?."$desc.entityName"?."authentication"
                          ?: "")
            if(name == "") {
                log.info "No authenticator setted up, skipping"
                return null
            }
            try {
                authenticator["${desc.entityName} ${desc.operation}"] = Class.forName("groovy.org.grails.datastore.remote.hibernate.auth.${name}Authenticator")?.newInstance(desc.entityName, desc.operation)
            }   catch(ClassNotFoundException ex)    {
                log.error "Class auth.${name}Authenticator does not exist!"
                return null
            }
        }
        return authenticator["${desc.entityName} ${desc.operation}"]
    }

    /**
     * Gets authentication params based on configuration for entity and operation
     * @param entity entity to be authenticated
     * @param operation operation to be authenticated
     * @return instance map of authentication params
     */
    static Map<String, Object> getAuthenticationParams(String entity, Operation operation) {
        if(!authenticationParams["${entity} ${operation}"])  {
            authenticationParams["${entity} ${operation}"] = (mapping?."$entity"?."operations"?.getAt(operation)?."authenticationParams"
                    ?: mapping[entity]["authenticationParams"]
                    ?: [:])
        }

        return authenticationParams["${entity} ${operation}"]
    }

    /**
     * Checks if operation is allowed from configuration
     * @param desc query descriptor
     * @return true or false
     */
    static boolean isOperationAllowed(QueryDescriptor desc) {
        if(!isValidDescriptor(desc))
            return false
        return (!mapping?.containsKey(desc.entityName) || !mapping?."${desc.entityName}"?.containsKey("allowed") || mapping?."${desc.entityName}"?."allowed" == null || mapping[desc.entityName]["allowed"].contains(desc.operation))
    }

    /**
     * Checks if given descriptor is valid, that means, it has
     * non empty entityName and also it has operation.
     * @param desc query descriptor to be checked
     * @return true or false
     */
    private static boolean isValidDescriptor(QueryDescriptor desc) {
        if (!desc?.entityName || desc?.entityName?.empty) {
            log.error "Descriptor entityName is required"
            return false
        }
        if (!desc.operation) {
            log.error "Descriptor operation is required"
            return false
        }
        return true
    }

    /**
     * Gets attribute mapping based on configuration for query
     * @param desc query descriptor
     * @return attribute mapping
     */
    static Map<String, String> getAttributeMap(QueryDescriptor desc)   {
        if(!isValidDescriptor(desc))
            return null
        if(!attributeMapping[desc.entityName]) {
            attributeMapping[desc.entityName] = [:]
            try {
                new DefaultGrailsDomainClass(Class.forName(desc.entityName))?.properties?.each { it ->
                    if(it.name != "version" && !mapping?."$desc.entityName"?."local"?.contains(it.name))
                        attributeMapping?."$desc.entityName"?."${it.name}" = mapping?."$desc.entityName"?."mapping"?."${it.name}" ?: it.name
                }
            }   catch(ClassNotFoundException ex)    {
                log.error "Class ${desc.entityName} does not exist!"
                return null
            }   catch(GrailsDomainException ex) {
                log.error "Class ${desc.entityName} is not a domain class!"
                return null
            }
        }
        return attributeMapping[desc.entityName]
    }

    /**
     * Gets Map containing setting for operation based on query.
     * @param desc query descriptor
     * @return Map with operation settings
     */
    static Map<String, Object> getQueryOperation(QueryDescriptor desc)   {
        if(!isOperationAllowed(desc))   {
            println "Operation ${desc.operation} for ${desc.entityName} is not allowed"
            log.warn "Operation ${desc.operation} for ${desc.entityName} is not allowed"
            return null
        }

        ApplicationContext context = ServletContextHolder.servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT) as ApplicationContext
        def defaultConfig = context.getBean(GrailsApplication).config.grails.plugins.hibernateRemoteDatasource.defaults
        Map<String, Object> tempOperation = [:]
        defaultConfig?.generalDefault?.each  {
            tempOperation[it?.key] = it?.value
        }
        defaultConfig?.operations?."${desc.operation}"?.each  {
            tempOperation[it?.key] = it?.value
        }

        mapping?."${desc?.entityName}"?."generalDefault"?.each {
            tempOperation[it?.key] = it?.value
        }
        mapping?."${desc?.entityName}"?."operations"?.getAt(desc?.operation)?.each {
            tempOperation[it?.key] = it?.value
        }
        if(tempOperation?."endpoint") {
            tempOperation?."endpoint" = tempOperation?."endpoint"?.replaceAll(/\[:operation(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(tempOperation?."endpoint", "operation", desc.operation.toString())}")
            tempOperation?."endpoint" = tempOperation?."endpoint"?.replaceAll(/\[:entityName(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(tempOperation?."endpoint", "entityName", desc.entityName.tokenize('.')?.last())}")
        }
        if(tempOperation?."queryEndpoint") {
            tempOperation?."queryEndpoint" = tempOperation?."queryEndpoint"?.replaceAll(/\[:operation(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(tempOperation?."queryEndpoint", "operation", desc.operation.toString())}")
            tempOperation?."queryEndpoint" = tempOperation?."queryEndpoint"?.replaceAll(/\[:entityName(\|[a-zA-z1-9_-]*(<<'?[a-zA-z1-9_:' .-]*'?)*)*\]/, "${Formatter.formatAttribute(tempOperation?."queryEndpoint", "entityName", desc.entityName.tokenize('.')?.last())}")
        }
        if(!mapping?."$desc.entityName"?."operations")
            mapping?."$desc.entityName"?."operations" = [:]
        mapping?."$desc.entityName"?."operations"?.putAt(desc.operation, tempOperation)
        mapping?."$desc.entityName"?."operations"?.getAt(desc.operation)
    }
}
