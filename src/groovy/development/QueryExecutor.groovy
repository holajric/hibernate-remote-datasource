package development

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import parsers.config.CachedConfigParser
import query.QueryDescriptor

@Transactional
class QueryExecutor {
    static boolean executeQuery(QueryDescriptor desc)  {
        if(CachedConfigParser.isOperationAllowed(desc)) {
            def remoteQuery = CachedConfigParser.getQueryBuilder(desc).generateQuery(desc)
            def connector = CachedConfigParser.getDataSourceConnector(desc)
            List<JSONObject> responses = connector.read(remoteQuery)
            println responses
            def mapping = CachedConfigParser.getAttributeMap(desc)
            println mapping
            responses.each { response ->
                def instance = Class.forName(desc.entityName).get(response[mapping["id"]])?:Class.forName(desc.entityName).newInstance()
                mapping.each {
                    if (response["$it.value"]) {
                        instance."$it.key" = response["$it.value"]
                    }
                }
                instance.save()
            }
        }
        true
    }
}
