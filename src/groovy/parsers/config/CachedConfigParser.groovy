package parsers.config

import connectors.DataSourceConnector
import grails.transaction.Transactional
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.context.ApplicationContext
import query.Operation
import query.QueryDescriptor
import query.builder.QueryBuilder
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import auth.Authenticator
import query.builder.formatters.Formatter

@Log4j
@Transactional
class CachedConfigParser {
    static Map<String, DataSourceConnector> dataSourceConnector = new HashMap<String, DataSourceConnector>()
    static Map<String, QueryBuilder> queryBuilder = new HashMap<String, QueryBuilder>()
    static Map<String, Authenticator> authenticator = new HashMap<String, Authenticator>()
    static Map<String, Object> mapping  = new HashMap<String, Object>()
    static Map<String, Map<String, String>> attributeMapping  = new HashMap<String, Map<String, String>>()
    static Map<String, Map<String, Object>> authenticationParams  = new HashMap<String, Map<String, Object>>()

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
        mapping[entity.getName()] != null
    }

    static DataSourceConnector getDataSourceConnector(QueryDescriptor desc)    {
        if(desc.entityName.empty)   {
            log.info "Descriptor entityName is required"
            return null
        }
        if(!mapping[desc.entityName]["sourceType"])   {
            log.info "DataSourceConnector type for class has to be set"
            return null
        }
        if(!dataSourceConnector[desc.entityName])
            try {
                dataSourceConnector[desc.entityName] = Class.forName("connectors."+mapping[desc.entityName]["sourceType"]+"DataSourceConnector")?.newInstance()
            }   catch(ClassNotFoundException ex)    {
                log.info "Class connectors."+mapping[desc.entityName]["sourceType"]+"DataSourceConnector does not exist!"
                return null
            }
        return dataSourceConnector[desc.entityName]
    }

    static QueryBuilder getQueryBuilder(QueryDescriptor desc)  {
        if(desc.entityName.empty)   {
            log.info "Descriptor entityName is required"
            return null
        }
        if(!mapping[desc.entityName]["queryType"])   {
            log.info "QueryBuilder type for class has to be set"
            return null
        }
        if(!queryBuilder[desc.entityName])
            try {
                queryBuilder[desc.entityName] = Class.forName("query.builder."+mapping[desc.entityName]["queryType"]+"QueryBuilder")?.newInstance()
            }   catch(ClassNotFoundException ex)    {
                log.info "Class query.builder."+mapping[desc.entityName]["queryType"]+"QueryBuilder does not exist!"
                return null
            }
        return queryBuilder[desc.entityName]
    }

    static Authenticator getAuthenticator(QueryDescriptor desc) {
        if(!authenticator["${desc.entityName} ${desc.operation}}"])  {
            String name = (mapping?."$desc.entityName"?."operations"?.getAt(desc.operation)?."authentication"
                          ?: mapping[desc.entityName]["authentication"]
                          ?: "")
            if(name == "")
                return null
            authenticator["${desc.entityName} ${desc.operation}}"] = Class.forName("auth.${name}Authenticator")?.newInstance(desc.entityName, desc.operation)
        }

        return authenticator["${desc.entityName} ${desc.operation}}"]
    }

    static Map<String, Object> getAuthenticationParams(String entity, Operation operation) {
        if(!authenticationParams["${entity} ${operation}}"])  {
            authenticationParams["${entity} ${operation}}"] = (mapping?."$entity"?."operations"?.getAt(operation)?."authenticationParams"
                    ?: mapping[entity]["authenticationParams"]
                    ?: [:])
        }

        return authenticationParams["${entity} ${operation}}"]
    }

    static boolean isOperationAllowed(QueryDescriptor desc) {
        return (mapping[desc.entityName]["allowed"] == null || mapping[desc.entityName]["allowed"].contains(desc.operation))
    }

    static Map<String, String> getAttributeMap(QueryDescriptor desc)   {
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
        ApplicationContext context = ServletContextHolder.servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT) as ApplicationContext
        def defaultConfig = context.getBean(GrailsApplication).config.grails.plugins.hibernateRemoteDatasource.defaults
        Map<String, Object> tempOperation = [:]
        defaultConfig.generalDefault.each  {
            tempOperation[it.key] = it.value
        }
        defaultConfig.operations?."${desc.operation}".each  {
            tempOperation[it.key] = it.value
        }

        //println tempOperation
        mapping?."$desc.entityName"?."generalDefault".each {
            tempOperation[it.key] = it.value
        }
        mapping?."$desc.entityName"?."operations"?.getAt(desc.operation).each {
            tempOperation[it.key] = it.value
        }
        //println Formatter.formatAttribute(tempOperation?."endpoint","entityName", desc.entityName.tokenize('.')?.last())
        tempOperation?."endpoint" = tempOperation?."endpoint"?.replaceAll(/\[:operation(\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*\]/, "${Formatter.formatAttribute(tempOperation?."endpoint","operation", desc.operation.toString())}")
        tempOperation?."endpoint" = tempOperation?."endpoint"?.replaceAll(/\[:entityName(\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*\]/, "${Formatter.formatAttribute(tempOperation?."endpoint","entityName", desc.entityName.tokenize('.')?.last())}")
        tempOperation?."queryEndpoint" = tempOperation?."queryEndpoint"?.replaceAll(/\[:operation(\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*\]/, "${Formatter.formatAttribute(tempOperation?."queryEndpoint","operation", desc.operation.toString())}")
        tempOperation?."queryEndpoint" = tempOperation?."queryEndpoint"?.replaceAll(/\[:entityName(\|[a-zA-z1-9_-]*(:'?[a-zA-z1-9_-]*'?)*)*\]/, "${Formatter.formatAttribute(tempOperation?."queryEndpoint","entityName", desc.entityName.tokenize('.')?.last())}")
        //println tempOperation
        if(!mapping?."$desc.entityName"?."operations")
            mapping?."$desc.entityName"?."operations" = [:]
        mapping?."$desc.entityName"?."operations"?.putAt(desc.operation, tempOperation)
        mapping?."$desc.entityName"?."operations"?.getAt(desc.operation)
    }
}
