package groovy.org.grails.datastore.remote.hibernate.connectors

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import groovy.org.grails.datastore.remote.hibernate.query.builder.*
import groovy.org.grails.datastore.remote.hibernate.auth.Authenticator
import org.springframework.http.ResponseEntity

/**
 * Created by richard on 18.2.15.
 */
@Log4j
class RestDataSourceConnector implements DataSourceConnector {
    def rest = new RestBuilder()
    final static String ALLOWED_METHODS = ["get", "post", "put", "delete", "patch", "trace", "head", "options"]

    boolean doAction(RemoteQuery query, Authenticator auth = null)  {
        def response = makeCall(query, auth) {}
        return (response instanceof RestResponse)
    }

    List<JSONObject> read(RemoteQuery query, String className, Authenticator auth = null) {
        def response = makeCall(query, auth) {}
        return sanitizeResponse(response, className)
    }

    List<JSONObject> write(RemoteQuery query, String className, Authenticator auth = null)   {
        def response = makeCall(query, auth) {
            json query?.dataJson?.toString()
            contentType "application/json"
        }
        return sanitizeResponse(response, className)
    }

    private def makeCall(RemoteQuery query, Authenticator auth = null, Closure defaultRequest)  {
        def methodName = sanitizeInput(query, auth)
        if(methodName == false)
            return null
        def requestBody = auth?.getAuthenticatedBody(query) ?: defaultRequest
        def response = rest."$methodName"(query.url, requestBody)
        log.info "${response?.getStatus()} $query"
        return response
    }

    private Object sanitizeInput(RemoteQuery query, Authenticator auth = null )  {
        if(!(query instanceof RestRemoteQuery)) {
            log.error "groovy.org.grails.datastore.remote.hibernate.query $query is not instance of RestRemoteQuery which is required"
            return false
        }
        if(query.url.empty) {
            log.error "groovy.org.grails.datastore.remote.hibernate.query can not be empty"
            return false
        }
        if(auth && !auth?.authenticate(query)) {
            log.error "unauthorized for $query"
            return false
        }
        String methodName = query?.method?.toLowerCase()
        if(!ALLOWED_METHODS.contains(methodName))   {
            log.error "invalid http method $methodName"
            return false
        }
        return methodName
    }

    private List<JSONObject> sanitizeResponse(response, className) {
        try {
            if (response?.getStatus()?.toString()[0] == '4') {
                log.error "resource not found or not accepted"
                return null
            }
            if (response?.getStatus()?.toString()[0] == '5') {
                log.error "resource failed with error"
                return null
            }
            if (!(response instanceof RestResponse)) {
                return null
            }
        }   catch(MissingMethodException ex)    {
            log.error "invalid response"
            return null
        }
        if(!(onIndex(response.json, CachedConfigParser.mapping[className]["dataPrefix"]) instanceof JSONArray))
            return [onIndex(response.json, CachedConfigParser.mapping[className]["dataPrefix"])]
        List<JSONObject> responseList = []
        for(def i = 0; i < onIndex(response.json, CachedConfigParser.mapping[className]["dataPrefix"]).length(); i++)
            responseList.add(onIndex(response.json, CachedConfigParser.mapping[className]["dataPrefix"]).getJSONObject(i))

        return responseList
    }

    private static Object onIndex(collection, String dottedIndex)   {
        def result = collection
        if(dottedIndex == null || dottedIndex.empty)
            return result
        def indexes = dottedIndex.tokenize(".")
        indexes.each{
            result = result[it]
        }
        return result
    }
}
