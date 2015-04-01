package connectors

import query.builder.RemoteQuery
import auth.Authenticator

/**
 * Created by richard on 18.2.15.
 */
public interface DataSourceConnector {
    boolean doAction(RemoteQuery query, Authenticator auth)
    Object read(RemoteQuery query, Authenticator auth)
    Object write(RemoteQuery query, Authenticator auth)
}