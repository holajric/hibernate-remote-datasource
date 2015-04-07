package groovy.org.grails.datastore.remote.hibernate.auth

import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import groovy.org.grails.datastore.remote.hibernate.query.builder.RemoteQuery

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
