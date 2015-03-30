package development

import grails.transaction.Transactional
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import parsers.config.CachedConfigParser
import query.QueryDescriptor
import query.Operation


@Transactional
class QueryExecutor {

    static boolean executeFinderQuery(QueryDescriptor desc)  {
        if(CachedConfigParser.isOperationAllowed(desc)) {
            def remoteQuery = CachedConfigParser.getQueryBuilder(desc).generateQuery(desc)
            def connector = CachedConfigParser.getDataSourceConnector(desc)
            List<JSONObject> responses = connector.read(remoteQuery)
            return processResponses(responses, desc)
        }
        return true
    }

    static boolean executeInstanceQuery(QueryDescriptor desc, Object instance)   {
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
            return processResponses(responses, desc, instance)
        }
        return true
    }

     private static boolean processResponses(List<JSONObject> responses, QueryDescriptor desc, instance = null)   {
        def mapping = CachedConfigParser.getAttributeMap(desc)
        ResponseFilter filter = new ResponseFilter()
        responses.each { response ->
            if (filter.isValid(response, desc)) {
                def instanceTemp = instance ?: Class.forName(desc.entityName).newInstance()
                mapping.each {
                    if (response["$it.value"]) {
                        instanceTemp."$it.key" = response["$it.value"]
                    }
                }
                if(!instance)
                    instanceTemp.save()
            }
        }

        return true
    }

}
