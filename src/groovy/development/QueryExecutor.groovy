package development

import grails.transaction.Transactional
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import parsers.config.CachedConfigParser
import query.QueryDescriptor
import query.Operation


@Transactional
class QueryExecutor {

    static boolean executeQuery(QueryDescriptor desc, Object instance = null)  {
        println instance
        if(CachedConfigParser.isOperationAllowed(desc)) {
            def remoteQuery = CachedConfigParser.getQueryBuilder(desc).generateQuery(desc)
            if(instance) {
                if(!remoteQuery.dataJson)
                    remoteQuery.dataJson = [:]
                CachedConfigParser.attributeMapping[desc.entityName].each {
                    if(instance."$it.key") {
                        remoteQuery.dataJson."$it.value" =  instance."$it.key"
                    }
                }
                println remoteQuery.dataJson
            }
            def connector = CachedConfigParser.getDataSourceConnector(desc)
            List<JSONObject> responses
            if(desc.operation == Operation.READ) {
                responses = connector.read(remoteQuery)

                def mapping = CachedConfigParser.getAttributeMap(desc)
                ResponseFilter filter = new ResponseFilter()
                responses.each { response ->
                    if (filter.isValid(response, desc)) {
                        def instanceTemp = Class.forName(desc.entityName).get(response[mapping["id"]]) ?: Class.forName(desc.entityName).newInstance()
                        mapping.each {
                            if (response["$it.value"]) {
                                instanceTemp."$it.key" = response["$it.value"]
                            }
                        }
                        instanceTemp.save()
                    }
                }
            }

            if(desc.operation == Operation.CREATE) {
                println remoteQuery
                println connector.write(remoteQuery, remoteQuery.dataJson)
            }
        }

        true
    }
}
