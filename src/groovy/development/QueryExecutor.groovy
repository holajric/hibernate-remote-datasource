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
        if(CachedConfigParser.isOperationAllowed(desc)) {
            def mapping = CachedConfigParser.getAttributeMap(desc)
            def remoteQuery
            if((remoteQuery = CachedConfigParser.getQueryBuilder(desc)?.generateQuery(desc)) == null)   {
                return false
            }
            remoteQuery.dataJson = [:]
            //println "startMapping"
            mapping.each {
                //println "${it.key}: ${it.value}"
                if(instance."$it.key") {
                    remoteQuery.dataJson."$it.value" =  instance."$it.key"
                }
            }
            //println "endMapping"
            def connector
            if((connector = CachedConfigParser.getDataSourceConnector(desc)) == null)   {
                return false
            }
            if(desc.operation == Operation.DELETE)  {
                //println "DELETE - DO ACTION"
                return connector.doAction(remoteQuery, CachedConfigParser.getAuthenticator(desc))
                //println "DELETE - END ACTION"
            }
            List<JSONObject> responses
            if((responses = connector.write(remoteQuery, CachedConfigParser.getAuthenticator(desc))) == null)    {
                log.info "Data could not be read from ${remoteQuery}"
                return false
            }
            return processResponses(responses, desc, instance)
        }
        return true
    }

    private static boolean isValidDescriptor(QueryDescriptor desc)    {

    }

    private static boolean processResponses(List<JSONObject> responses, QueryDescriptor desc, instance = null)   {
        def mapping = CachedConfigParser.getAttributeMap(desc)
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
