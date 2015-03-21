package development

import grails.transaction.Transactional
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONElement
import parsers.config.CachedConfigParser
import query.QueryDescriptor
import grails.converters.JSON
import groovy.json.JsonSlurper

@Transactional
class QueryExecutor {
    static boolean executeQuery(QueryDescriptor desc)  {
        if(CachedConfigParser.isOperationAllowed(desc)) {
            def remoteQueries = CachedConfigParser.getQueryBuilder(desc).generateQueries(desc)
            def connector = CachedConfigParser.getDataSourceConnector(desc)
            List<JSONObject> responses = []
            /*remoteQueries.eachWithIndex { query, index ->

                List<JSONObject> newObj = connector.read(query)
                newObj.eachWithIndex { JSONObject entry, int i ->
                    oldObj = responses.get(i)
                    oldObj ? {

                    } : responses.add(i, entry)
                }

                JSONObject merged = new JSONObject(responses.get(index), JSONObject.getNames(responses.get(index)))
            }*/
            println responses
            def mapping = CachedConfigParser.getAttributeMap(desc)
            println mapping
            //TODO ResponseParser
            responses
            /*responses.each { response ->
                def instance = Class.forName(desc.entityName).get(response[mapping["id"]])?:Class.forName(desc.entityName).newInstance()
                mapping.each {
                    if (response["$it.value"]) {
                        instance."$it.key" = response["$it.value"]
                    }
                }
                instance.save()
            }*/
        }
        true
    }
}
