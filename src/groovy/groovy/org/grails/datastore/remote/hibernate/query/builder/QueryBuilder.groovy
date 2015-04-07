package groovy.org.grails.datastore.remote.hibernate.query.builder

import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor

/**
 * Created by richard on 18.2.15.
 */
public interface QueryBuilder {
    RemoteQuery generateQuery(QueryDescriptor desc)
}