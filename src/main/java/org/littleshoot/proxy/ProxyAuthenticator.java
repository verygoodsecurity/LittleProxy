package org.littleshoot.proxy;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Interface for objects that can authenticate someone for using our Proxy on
 * the basis of authorization header.
 */
public interface ProxyAuthenticator {

    /**
     * Authenticates the user using the specified proxy authorization header.
     *
     * @param httpRequest
     *            http request
     *
     * @return <code>true</code> if the credential is acceptable, otherwise
     *         <code>false</code>.
     */
    boolean authenticate(HttpRequest httpRequest);

    /**
     * The realm value to be used in the request for proxy authentication 
     * ("Proxy-Authenticate" header). Returning null will cause the string
     * "Restricted Files" to be used by default.
     * 
     * @return
     */
    String getRealm();

    /**
     * Response that is going to be returned on authentication failure
     */
    FullHttpResponse authenticationFailureResponse(HttpRequest request);
}
