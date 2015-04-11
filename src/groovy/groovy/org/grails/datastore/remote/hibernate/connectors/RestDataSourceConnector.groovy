package groovy.org.grails.datastore.remote.hibernate.connectors

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
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
        def methodName = sanitizeInput(query, auth)
        if(methodName == false)
            return false
        def requestBody = auth?.getAuthenticatedBody(query) ?: {}
        def response = rest."$methodName"(query?.url, requestBody)
        log.info "${response.getStatus()} $query"
        return (response instanceof RestResponse)
    }

    List<JSONObject> read(RemoteQuery query, Authenticator auth = null)  {
        def methodName = sanitizeInput(query, auth)
        if(methodName == false)
            return null
        def requestBody = auth?.getAuthenticatedBody(query) ?: {}
        def response = rest."$methodName"(query.url, requestBody)
        log.info "${response?.getStatus()} $query"
        return sanitizeResponse(response)
    }

    List<JSONObject> write(RemoteQuery query, Authenticator auth = null)   {
        def methodName = sanitizeInput(query, auth)
        if(methodName == false)
            return null
        def requestBody = auth?.getAuthenticatedBody(query) ?: {
            json query?.dataJson?.toString()
            contentType "application/json"
        }
        def response = rest."$methodName"(query?.url, requestBody)
        log.info "${response?.getStatus()} $query"
        return sanitizeResponse(response)
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

    private List<JSONObject> sanitizeResponse(response) {
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
        if(!(response.json instanceof JSONArray))
            return [response.json]
        List<JSONObject> responseList = []
        for(def i = 0; i < response?.json?.length(); i++)
            responseList.add(response?.json?.getJSONObject(i))

        return responseList
    }
}
