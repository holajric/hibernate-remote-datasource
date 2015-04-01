package auth

import parsers.config.CachedConfigParser
import query.Operation
import query.builder.RemoteQuery

/**
 * Created by richard on 1.4.15.
 */
class TokenAuthenticator implements Authenticator   {
    private Map<String, Object> authenticationParameters

    public TokenAuthenticator(String entity, Operation operation)  {
        this.authenticationParameters = CachedConfigParser.getAuthenticationParams(entity, operation)
    }

    boolean authenticate(RemoteQuery query) {
        return true
    }

    Closure getAuthenticatedBody(RemoteQuery query) {
        return {
            json query.dataJson.toString()
            contentType "application/json"
            header 'X-Auth-Token', authenticationParameters["token"] ?:""
        }
    }
}
