package query.builder

import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by richard on 18.3.15.
 */
class RestRemoteQuery implements RemoteQuery {
    String url
    String method
    JSONObject dataJson

    String toString()   {
        "$method $url $dataJson"
    }
}
