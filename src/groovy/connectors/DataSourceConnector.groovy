package connectors

import query.builder.RemoteQuery

/**
 * Created by richard on 18.2.15.
 */
public interface DataSourceConnector {
    boolean doAction(RemoteQuery query)
    Object read(RemoteQuery query)
    Object write(RemoteQuery query)
}