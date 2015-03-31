package development

import grails.transaction.Transactional
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import parsers.config.CachedConfigParser
import query.QueryDescriptor
import query.Operation
import synchronisation.JournalLog


@Transactional
class QueryExecutor {

    static boolean executeFinderQuery(QueryDescriptor desc)  {
        if(CachedConfigParser.isOperationAllowed(desc)) {
            def remoteQuery = CachedConfigParser.getQueryBuilder(desc).generateQuery(desc)
            //println remoteQuery
            def connector = CachedConfigParser.getDataSourceConnector(desc)
            //println connector.toString()+" readStart"
            List<JSONObject> responses = connector.read(remoteQuery)
            //println "readEnd"
            //println responses
            //println "startProc"
            def res = processResponses(responses, desc)
            //println "endProc"
            return res
        }
        return true
    }

    static boolean executeInstanceQuery(QueryDescriptor desc, Object instance)   {
        if(CachedConfigParser.isOperationAllowed(desc)) {
            def mapping = CachedConfigParser.getAttributeMap(desc)
            //println "building..."
            def remoteQuery = CachedConfigParser.getQueryBuilder(desc).generateQuery(desc)
            //println "builded: ${remoteQuery}"
            remoteQuery.dataJson = [:]
            //println "startMapping"
            mapping.each {
                //println "${it.key}: ${it.value}"
                if(instance."$it.key") {
                    remoteQuery.dataJson."$it.value" =  instance."$it.key"
                }
            }
            //println "endMapping"
            def connector = CachedConfigParser.getDataSourceConnector(desc)
            if(desc.operation == Operation.DELETE)  {
                //println "DELETE - DO ACTION"
                return connector.doAction(remoteQuery)
                //println "DELETE - END ACTION"
            }
            //println "SENDING DATA..."
            List<JSONObject> responses = connector.write(remoteQuery)
            //println "DATA SEND, RESPONSE: ${responses}"
            //println "PROCESSING"
            return processResponses(responses, desc, instance)
        }
        return true
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
