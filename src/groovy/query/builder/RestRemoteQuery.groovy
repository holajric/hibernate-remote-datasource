package query.builder

import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by richard on 18.3.15.
 */
class RestRemoteQuery implements RemoteQuery {
    String url
    String method
    JSONObject dataJson

    Map<String, Object> getQueryParams()    {
        [ "url": url, "method": method, "data":dataJson ]
    }

    String toString()   {
        "$method $url $dataJson"
    }
}
