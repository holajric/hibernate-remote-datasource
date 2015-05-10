package groovy.org.grails.datastore.remote.hibernate.auth

import groovy.org.grails.datastore.remote.hibernate.query.builder.RemoteQuery

/**
 * Interface defining classes used for authentication
 */
public interface Authenticator {
    /**
     * Method for pre-query authentication, that means it is executed before
     * query itself and query is executed only if authentication is successful.
     * @param query provided query that should be authenticated
     * @return result of authentications successful or not
     */
    public boolean authenticate(RemoteQuery query)
    /**
     * Method for authentication along with query, it add authentication
     * parameters to query request body, so they will be send with it.
     * @param query provided query that should be authenticated
     * @return Closure containing data, that will be sent as query,
     * including authentication parameters
     */
    public Closure getAuthenticatedBody(RemoteQuery query)
}