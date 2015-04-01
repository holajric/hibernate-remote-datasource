package connectors

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import query.builder.*
import auth.Authenticator
/**
 * Created by richard on 18.2.15.
 */
class RestDataSourceConnector implements DataSourceConnector {
    def rest = new RestBuilder()

    boolean doAction(RemoteQuery query, auth.Authenticator auth = null)  {
        if(auth && !auth?.authenticate(query))
            return []
        def requestBody = auth?.getAuthenticatedBody(query) ?: {}
        String methodName = query.method.toLowerCase()
        def response = rest."$methodName"(query.url, requestBody)
        return (response instanceof RestResponse)
    }

    List<JSONObject> read(RemoteQuery query, auth.Authenticator auth = null)  {
        if(auth && !auth?.authenticate(query))
            return []
        def requestBody = auth?.getAuthenticatedBody(query) ?: {}
        String methodName = query.method.toLowerCase()
        //println "transferStart"
        def response = rest."$methodName"(query.url, requestBody)
        //println "transferEnd sanitizeStart"
        def res = sanitizeResponse(response)
        //println "sanitizeEnd"
        return res
    }

    List<JSONObject> write(RemoteQuery query, Authenticator auth = null)   {
        String methodName = query.method.toLowerCase()
        if(auth && !auth?.authenticate(query))
            return []
        def requestBody = auth?.getAuthenticatedBody(query) ?: {
            json query.dataJson.toString()
            contentType "application/json"
        }
        def response = rest."$methodName"(query.url, requestBody)
        return sanitizeResponse(response)
    }

    private List<JSONObject> sanitizeResponse(response) {
        if(!(response instanceof RestResponse))    {
            return []
        }

        if(!(response.json instanceof JSONArray))
            return [response.json]

        List<JSONObject> responseList = []
        for(def i = 0; i < response.json.length(); i++)
            responseList.add(response.json.getJSONObject(i))

        return responseList
    }
}
