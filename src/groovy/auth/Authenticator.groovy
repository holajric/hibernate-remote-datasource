package auth

import query.Operation
import query.builder.RemoteQuery

/**
 * Created by richard on 1.4.15.
 */
public interface Authenticator {
    public boolean authenticate(RemoteQuery query)
    public Closure getAuthenticatedBody(RemoteQuery query)
}