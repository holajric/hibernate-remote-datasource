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
            println remoteQuery
            def connector = CachedConfigParser.getDataSourceConnector(desc)
            println connector.toString()+" readStart"
            List<JSONObject> responses = connector.read(remoteQuery)
            println "readEnd"
            println responses
            println "startProc"
            def res = processResponses(responses, desc)
            println "endProc"
            return res
        }
        return true
    }

    static Object executeInstanceQuery(QueryDescriptor desc, Object instance)   {
        if(CachedConfigParser.isOperationAllowed(desc)) {
            def mapping = CachedConfigParser.getAttributeMap(desc)
            def remoteQuery = CachedConfigParser.getQueryBuilder(desc).generateQuery(desc)
            remoteQuery.dataJson = [:]
            mapping.each {
                //println "${it.key}: ${it.value}"
                if(instance."$it.key") {
                    remoteQuery.dataJson."$it.value" =  instance."$it.key"
                }
            }
            def connector = CachedConfigParser.getDataSourceConnector(desc)
            if(desc.operation == Operation.DELETE)  {
                return connector.doAction(remoteQuery)
            }
            List<JSONObject> responses = connector.write(remoteQuery)
            println "BEFORE PROCES ${Class.forName(desc.entityName)?.count()}"
            processResponses(responses, desc, instance)
            return instance
        }
        return null
    }

     private static boolean processResponses(List<JSONObject> responses, QueryDescriptor desc, instance = null)   {
        def mapping = CachedConfigParser.getAttributeMap(desc)
        ResponseFilter filter = new ResponseFilter()
        responses.each { response ->
            if (filter.isValid(response, desc)) {
                if(instance != null)
                    instance.directDelete()
                println response
                def instanceTemp = Class.forName(desc.entityName)?.directGet(response[mapping["id"]]) ?: Class.forName(desc.entityName).newInstance()
                println instanceTemp
                if(!JournalLog.countByEntityAndInstanceIdAndIsFinished(desc.entityName, response[mapping["id"]], false)) {
                    SynchronizationManager.withTransaction(instanceTemp.class.name, response[mapping["id"]], desc.operation) {
                        println "transInStart"
                        instance = buildInstance(mapping, response, instanceTemp)
                        println "transInEnd"
                        return true
                    }
                }   else    {
                    instance = buildInstance(mapping, response, instanceTemp)
                }
            }
        }
         println "AFTER BUILD INSTANCE ${Class.forName(desc.entityName)?.count()}"

        return true
    }

    private static Object buildInstance(mapping, response, instanceTemp) {
        mapping.each {
            if (response["${it.value}"]) {
                instanceTemp."${it.key}" = response["${it.value}"]
            }
        }
        println "dirSaveStart"
        instanceTemp.directSave()
        println "dirSaveEnd"
        println instanceTemp
        return instanceTemp
    }

}
