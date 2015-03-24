package connectors

import query.builder.RemoteQuery

/**
 * Created by richard on 18.2.15.
 */
public interface DataSourceConnector {
    Object read(RemoteQuery query)
    Object write(RemoteQuery query, Object data)
}