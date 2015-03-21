package connectors

import query.builder.RemoteQuery

/**
 * Created by richard on 18.2.15.
 */
public interface DataSourceConnector {
    Object read(RemoteQuery query)
    boolean write(RemoteQuery query, Object data)
}