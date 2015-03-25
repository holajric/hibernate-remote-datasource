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

    List<JSONObject> read(RemoteQuery query)  {
        String methodName = query.method.toLowerCase()
        def response = rest."$methodName"(query.url)
        if(!(response instanceof RestResponse))    {
            return []
        }

        if(!(response.json instanceof JSONArray))
            return [response.json]
        List<JSONObject> responseList = []
        for(def i = 0; i < response.json.length(); i++)
            responseList.add(response.json.getJSONObject(i))
        responseList

    }

    JSONObject write(RemoteQuery query, Object data)   {
        String methodName = query.method.toLowerCase()
        def response = rest."$methodName"(query.url) {
            json query.dataJson.toString()
            contentType "application/json"
        }
        println response?.getStatus()
        return response?.json
    }
}
