package groovy.org.grails.datastore.remote.hibernate.connectors

import groovy.org.grails.datastore.remote.hibernate.query.builder.RemoteQuery
import org.codehaus.groovy.grails.web.json.JSONObject
import groovy.org.grails.datastore.remote.hibernate.auth.Authenticator

/**
 * This interface represents all connectors to remote datasources and defines
 * basic method they have implement. It allows to read, write or execute action
 * on remote data source.
 */
public interface DataSourceConnector {
    /**
     * Method for executing actions (like delete) on remote data source.
     * @param query query to be executed on remote data source
     * @param auth authenticator for action
     * @return success or failure of action on remote data source
     */
    boolean doAction(RemoteQuery query, Authenticator auth)

    /**
     * Method for reading data from remote data source.
     * @param query query to be executed on remote data source
     * @param className name of class query is executed on
     * @param auth authenticator for action
     * @return data returned from remote data source
     */
    List<JSONObject> read(RemoteQuery query, String className, Authenticator auth)

    /**
     * Method for writing data to remote data source.
     * @param query query to be executed on remote data source
     * @param className name of class query is executed on
     * @param auth authenticator for action
     * @return data returned from remote data source
     */
    List<JSONObject> write(RemoteQuery query, String className, Authenticator auth)
}