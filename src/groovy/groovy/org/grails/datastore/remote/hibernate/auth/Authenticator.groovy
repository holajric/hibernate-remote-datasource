package groovy.org.grails.datastore.remote.hibernate.auth

import groovy.org.grails.datastore.remote.hibernate.query.builder.RemoteQuery

/**
 * Created by richard on 1.4.15.
 */
public interface Authenticator {
    public boolean authenticate(RemoteQuery query)
    public Closure getAuthenticatedBody(RemoteQuery query)
}