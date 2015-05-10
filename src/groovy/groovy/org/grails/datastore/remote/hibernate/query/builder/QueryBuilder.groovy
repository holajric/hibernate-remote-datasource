package groovy.org.grails.datastore.remote.hibernate.query.builder

import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor

/**
 * Interface for all QueryBuilders. Right now it has only implementation for REST,
 * but same interface can be implemented for example for SOAP in future. It is
 * meant to build real query (based on descriptor, prefix and configuration) for remote source.
 */
public interface QueryBuilder {
    /**
     * This method generates structure containing query for specific remote source,
     * it is based on descriptor and it uses informations from configuration.
     * @param desc descriptor of query to be generated
     * @param prefix prefix is used to load special configuration for example
     * for authentication or hash queries
     * @return instance of RemoteQuery containing necessary informations and data for
     * querying remote data source
     */
    RemoteQuery generateQuery(QueryDescriptor desc, String prefix)
}