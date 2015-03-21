package query.builder

import query.QueryDescriptor

/**
 * Created by richard on 18.2.15.
 */
public interface QueryBuilder {
    List<RemoteQuery> generateQueries(QueryDescriptor desc)
}