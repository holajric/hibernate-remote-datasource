package connectors

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import query.builder.*
/**
 * Created by richard on 18.2.15.
 */
class RestDataSourceConnector implements DataSourceConnector {
    def rest = new RestBuilder()

    Object read(RemoteQuery query)  {
        String methodName = query.method.toLowerCase()
        println query.url
        def response = rest."$methodName"(query.url)
        if(!(response instanceof RestResponse))    {
            return null
        }
        if(response.json instanceof JSONObject)
            response.json
        List<JSONObject> responseList = []
        for(def i = 0; i < response.json.length(); i++)
            responseList.add(response.json.getJSONObject(i))
        println responseList
        responseList

    }

    boolean write(RemoteQuery query, Object data)   {
        String methodName = query.method.toLowerCase()
        def response = rest."$methodName"(query.url) { json: query.data }
        (response instanceof RestResponse) ? true : false
    }
}
