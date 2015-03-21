package query.builder

/**
 * Created by richard on 18.3.15.
 */
class RestRemoteQuery implements RemoteQuery {
    String url
    String method
    String dataJson

    Map<String, Object> getQueryParams()    {
        [ "url": url, "method": method, "data":dataJson ]
    }

    String toString()   {
        "$method $url $dataJson"
    }
}
