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
        if(!mapping["id"])    {
            mapping["id"] = "id"
        }
        ResponseFilter filter = new ResponseFilter()
        responses.each { response ->
            if(!response[mapping["id"]])    {
                log.info "There is no id in response, response can not be processed"
                return false
            }
            if (filter.isValid(response, desc)) {
                def instanceTemp
                try {
                    if((instanceTemp = instance ?: Class.forName(desc.entityName)?.directGet(response[mapping["id"]?:"id"]) ?: Class.forName(desc.entityName).newInstance()) == null) {
                        log.info "Instance could not be found or created"
                        return false
                    }
                }   catch(ClassNotFoundException ex)    {
                    log.info "Class ${desc.entityName} could not be found."
                    return false
                }   catch(MissingMethodException ex)    {
                    log.info "Class ${desc.entityName} is not domain."
                    return false
                }
                if(!JournalLog.countByEntityAndInstanceIdAndIsFinished(desc.entityName, response[mapping["id"]], false)) {
                    if(!SynchronizationManager.withTransaction(instanceTemp.class.name, response[mapping["id"]], desc.operation) {
                        if(!buildInstance(mapping, response, instanceTemp)) {
                            log.info "Instance ${instanceTemp} could not be builded from response ${response}"
                            return false
                        }
                        return true
                    })  {
                        log.info ""
                        return false
                    }
                }   else    {
                    if(!buildInstance(mapping, response, instanceTemp)) {
                        log.info "Instance ${instanceTemp} could not be builded from response ${response}"
                        return false
                    }
                }
            }
        }

        return true
    }

    private static boolean buildInstance(mapping, response, instanceTemp) {
        if(instanceTemp == null)    {
            log.info "Target instance is required"
            return false
        }
        mapping.each {
            try {
                if (response["${it.value}"]) {
                    instanceTemp?."${it.key}" = response["${it.value}"]
                } else {
                    log.info "Response for attribute ${it.key} mapped by ${it.value} empty, skipping"
                }
            } catch(MissingPropertyException ex)    {
                log.info "Attribute ${it.key} od ${instanceTemp} not found, skipping"
            }
        }
        try {
            instanceTemp.validate()
            if(instanceTemp.hasErrors())   {
                log.info "Instance could not be saved for following reasons ${instanceTemp.errors}"
                return false
            }
            instanceTemp.directSave()
            return true
        } catch(MissingMethodException ex)    {
            log.info "${instanceTemp} is not domain class"
            return false
        }
    }

}
