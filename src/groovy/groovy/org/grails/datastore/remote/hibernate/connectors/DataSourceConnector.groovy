package groovy.org.grails.datastore.remote.hibernate.connectors

import org.codehaus.groovy.grails.web.json.JSONObject
import groovy.org.grails.datastore.remote.hibernate.query.builder.RemoteQuery
import auth.Authenticator

/**
 * Created by richard on 18.2.15.
 */
public interface DataSourceConnector {
    boolean doAction(RemoteQuery query, Authenticator auth)
    List<JSONObject> read(RemoteQuery query, Authenticator auth)
    List<JSONObject> write(RemoteQuery query, Authenticator auth)
}