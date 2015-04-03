package development

import grails.transaction.Transactional
import grails.converters.JSON
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.web.json.JSONObject
import parsers.config.CachedConfigParser
import query.QueryDescriptor
import query.Operation
import synchronisation.JournalLog

@Log4j
@Transactional
class QueryExecutor {

    static boolean executeFinderQuery(QueryDescriptor desc)  {
        if(!isValidDescriptor(desc))    {
            log.info "Descriptor $desc is invalid"
            return false
        }
        if(!CachedConfigParser.isOperationAllowed(desc)) {
            log.info "Operation ${desc.operation} not allowed for class ${desc.entityName}"
            return false
        }
        def remoteQuery
        if((remoteQuery = CachedConfigParser.getQueryBuilder(desc)?.generateQuery(desc)) == null)   {
            log.info "RemoteQuery could not be created for ${desc.entityName} ${desc.operation}"
            return false
        }
        def connector
        if((connector = CachedConfigParser.getDataSourceConnector(desc)) == null)   {
            log.info "DataSourceConnector could not be loaded for ${desc.entityName} ${desc.operation}"
            return false
        }
        List<JSONObject> responses
        if((responses = connector.read(remoteQuery, CachedConfigParser.getAuthenticator(desc))) == null)    {
            log.info "Data could not be read from ${remoteQuery}"
            return false
        }
        return processResponses(responses, desc)
    }

    static boolean executeInstanceQuery(QueryDescriptor desc, Object instance)   {
        if(!isValidDescriptor(desc))    {
            log.info "Descriptor $desc is invalid"
            return false
        }
        if(instance == null)    {
            log.info "Instance is required"
            return false
        }
        if(!CachedConfigParser.isOperationAllowed(desc)) {
            log.info "Operation ${desc.operation} not allowed for class ${desc.entityName}"
            return false
        }
        def mapping
        if((mapping = CachedConfigParser.getAttributeMap(desc)) == null)    {
            log.info "Mapping for class ${desc.entityName} could not be loaded"
            return false
        }
        def remoteQuery
        if((remoteQuery = CachedConfigParser.getQueryBuilder(desc)?.generateQuery(desc)) == null)   {
            log.info "RemoteQuery could not be created for ${desc.entityName} ${desc.operation}"
            return false
        }
        remoteQuery.dataJson = [:]
        mapping.each {
            if(!instance?."$it.key") {
                log.info "Instance ${instance} does not have an attribute ${it.key}, skipping"
            }   else    {
                remoteQuery?.dataJson?."$it.value" =  instance?."$it.key"
            }
        }
        def connector
        if((connector = CachedConfigParser.getDataSourceConnector(desc)) == null)   {
            log.info "DataSourceConnector could not be loaded for ${desc.entityName} ${desc.operation}"
            return false
        }
        if(desc.operation == Operation.DELETE)  {
            if(connector.doAction(remoteQuery, CachedConfigParser.getAuthenticator(desc)) == false) {
                log.info "Action could not be done from ${remoteQuery}"
                return false
            }
            return true
        }
        List<JSONObject> responses
        if((responses = connector.write(remoteQuery, CachedConfigParser.getAuthenticator(desc))) == null)    {
            log.info "Data could not be read from ${remoteQuery}"
            return false
        }
        return processResponses(responses, desc, instance)
    }

    private static boolean isValidDescriptor(QueryDescriptor desc)    {
        if(!desc.entityName || desc.entityName.empty)   {
            log.info "Descriptor entityName is required"
            return false
        }

        if(!desc.operation)    {
            log.info "Descriptor operation is required"
            return false
        }

        return true
    }

    private static boolean processResponses(List<JSONObject> responses, QueryDescriptor desc, instance = null)   {
        def mapping
        if((mapping = CachedConfigParser.getAttributeMap(desc)) == null)    {
            log.info "Mapping for class ${desc.entityName} could not be loaded"
            return false
        }
        ResponseFilter filter = new ResponseFilter()
        responses.each { response ->
            if (filter.isValid(response, desc)) {
                //println response
                def instanceTemp = instance ?: Class.forName(desc.entityName)?.directGet(response[mapping["id"]]) ?: Class.forName(desc.entityName).newInstance()
                //println instanceTemp
                if(!JournalLog.countByEntityAndInstanceIdAndIsFinished(desc.entityName, response[mapping["id"]], false)) {
                    SynchronizationManager.withTransaction(instanceTemp.class.name, response[mapping["id"]], desc.operation) {
                        //println "transInStart"
                        buildInstance(mapping, response, instanceTemp)
                        //println "transInEnd"
                        return true
                    }
                }   else    {
                    buildInstance(mapping, response, instanceTemp)
                }
            }
        }

        return true
    }

    private static boolean buildInstance(mapping, response, instanceTemp) {
        mapping.each {
            if (response["${it.value}"]) {
                instanceTemp."${it.key}" = response["${it.value}"]
            }
        }
        //println "dirSaveStart"
        instanceTemp.directSave()
        //println "dirSaveEnd"
        //println instanceTemp
    }

}
