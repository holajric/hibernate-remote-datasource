package groovy.org.grails.datastore.remote.hibernate.query.builder

import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Class defining neccessary information for querying remote
 * REST data source.
 */
class RestRemoteQuery implements RemoteQuery {
    /**
     * URL of REST resource
     */
    String url
    /**
     * HTTP method used for REST resource
     */
    String method

    /**
     * Data to be sent with query
     */
    JSONObject dataJson

    /**
     * Simple string representation of object
     * @return string representation of object
     */
    String toString()   {
        "$method $url $dataJson"
    }
}
