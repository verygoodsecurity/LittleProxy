package org.littleshoot.proxy;

import com.google.common.io.BaseEncoding;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;

/**
 * Basic proxy authenticator that can authenticate someone for using our Proxy on
 * the basis of of a username and password
 */
public abstract class BasicProxyAuthenticator implements ProxyAuthenticator {

    @Override
    public boolean authenticate(String proxyAuthorizationHeaderValue, HttpRequest httpRequest) {
        String value = StringUtils.substringAfter(proxyAuthorizationHeaderValue, "Basic ").trim();

        byte[] decodedValue = BaseEncoding.base64().decode(value);

        String decodedString = new String(decodedValue, Charset.forName("UTF-8"));

        String userName = StringUtils.substringBefore(decodedString, ":");
        String password = StringUtils.substringAfter(decodedString, ":");

        return authenticate(userName, password, httpRequest);
    }

    /**
     * Authenticates the user using the specified userName and password.
     *
     * @param username
     *            The user name.
     * @param password
     *            The password.
     * @return <code>true</code> if the credentials are acceptable, otherwise
     *         <code>false</code>.
     * requests.
     */
    public abstract boolean authenticate(String username, String password, HttpRequest httpRequest);

    abstract public String getRealm();
}
