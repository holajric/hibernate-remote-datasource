package groovy.org.grails.datastore.remote.hibernate.auth

import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import groovy.org.grails.datastore.remote.hibernate.query.builder.RemoteQuery

/**
 * Implementation of Authenticator for authentication by unique user token
 */
class TokenAuthenticator implements Authenticator   {
    /**
     * Configurable parameters for authentication for example token
     */
    private Map<String, Object> authenticationParameters

    /**
     * Loads proper authentication params from configuration.
     * @param entity name of entity authenticator is for
     * @param operation name of operation authenticator is for
     */
    public TokenAuthenticator(String entity, Operation operation)  {
        this.authenticationParameters = CachedConfigParser.getAuthenticationParams(entity, operation)
    }

    /**
     * TokenAuthenticator has no pre-query authentication, thus this method always returns true.
     * @param query not relevant
     * @return always true
     */
    boolean authenticate(RemoteQuery query) {
        return true
    }

    /**
     * It creates standart request body for query and add X-Auth-Token header with appropriate value
     * of token, that was loaded from configuration in constructor or with no token, if there was no
     * token loaded.
     * @param query provided query that should be authenticated
     * @return query request body with header X-Auth-Token and token loaded from configuration
     */
    Closure getAuthenticatedBody(RemoteQuery query) {
        return {
            json query.dataJson.toString()
            contentType "application/json"
            header 'X-Auth-Token', authenticationParameters["token"] ?:""
        }
    }
}
