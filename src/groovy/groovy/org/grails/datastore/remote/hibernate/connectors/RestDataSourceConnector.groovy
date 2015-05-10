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
 * This class provides functionality to connect to REST remote datasource
 * and read, write and execute action on it.
 */
@Log4j
class RestDataSourceConnector implements DataSourceConnector {
    /** Class providing services for connecting itself **/
    def rest = new RestBuilder()
    /** List of allowed HTTP methods **/
    final static String ALLOWED_METHODS = ["get", "post", "put", "delete", "patch", "trace", "head", "options"]

    /**
     * Method for executing actions (like delete) on remote REST data source.
     * @param query query to be executed on remote REST data source
     * @param auth authenticator for action (nullable)
     * @return success or failure of action on remote REST data source
     */
    boolean doAction(RemoteQuery query, Authenticator auth = null)  {
        def response = makeCall(query, auth) {}
        return (response instanceof RestResponse)
    }

    /**
     * Method for reading data from remote REST data source.
     * @param query query to be executed on remote REST data source
     * @param className name of class query is executed on
     * @param auth authenticator for action (nullable)
     * @return data returned from remote REST data source
     */
    List<JSONObject> read(RemoteQuery query, String className, Authenticator auth = null) {
        def response = makeCall(query, auth) {}
        return sanitizeResponse(response, className)
    }

    /**
     * Method for writing data to remote REST data source.
     * @param query query to be executed on remote REST data source
     * @param className name of class query is executed on
     * @param auth authenticator for action (nullable)
     * @return data returned from remote REST data source
     */
    List<JSONObject> write(RemoteQuery query, String className, Authenticator auth = null)   {
        def response = makeCall(query, auth) {
            json query?.dataJson?.toString()
            contentType "application/json"
        }
        return sanitizeResponse(response, className)
    }

    /**
     * This method executes connection and executing of request on remote
     * REST data source.
     * @param query query to be executed on remote REST data source
     * @param auth authenticator for action (nullable)
     * @param defaultRequest default request body for action
     * @return data returned from remote REST data source or null if fails
     */
    private def makeCall(RemoteQuery query, Authenticator auth = null, Closure defaultRequest)  {
        def methodName = sanitizeInput(query, auth)
        if(methodName == false)
            return null
        def requestBody = auth?.getAuthenticatedBody(query) ?: defaultRequest
        def response = rest."$methodName"(query.url, requestBody)
        log.info "${response?.getStatus()} $query"
        return response
    }

    /**
     * This method checks if query contains all necessary attributes (URL, method),
     * if they are valid and if query itself is valid REST query - instance
     * of RestRemoteQuery. It also executes pre-execution authentication.
     * @param query query to be executed on remote REST data source
     * @param auth authenticator for action (nullable)
     * @return
     */
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

    /**
     * This method sanitizes response acquired from remote REST data source,
     * it checks status code and if response is instance of JSONArray or JSONObject.
     * In case of JSONArray it transforms it into List of JSONObjects.
     * @param response response from remote REST data source
     * @param className  name of class query is executed on
     * @return sanitized response from remote REST data source
     */
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

    /**
     * Gets data from multiple indexed position in collection
     * identified by index in dotted format.
     * @param collection collection data should be retrieved from
     * @param dottedIndex position identifier in pattern index1.index2
     * @return value on position or false if there is no value
     */
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
