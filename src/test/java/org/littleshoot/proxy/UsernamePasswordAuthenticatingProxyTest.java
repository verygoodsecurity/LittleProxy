package org.littleshoot.proxy;

import io.netty.handler.codec.http.HttpRequest;

/**
 * Tests a single proxy that requires username/password authentication.
 */
public class UsernamePasswordAuthenticatingProxyTest extends BaseProxyTest {
    private static String USERNAME = "user1";
    private static String PASSWORD = "password";

    @Override
    protected void setUp() {
        this.proxyServer = bootstrapProxy()
                .withPort(0)
                .withProxyAuthenticator(new TestBasicProxyAuthenticator())
                .start();
    }

    @Override
    protected String getUsername() {
        return USERNAME;
    }

    @Override
    protected String getPassword() {
        return PASSWORD;
    }

    @Override
    protected boolean isAuthenticating() {
        return true;
    }

    static class TestBasicProxyAuthenticator extends BasicProxyAuthenticator {

        @Override
        public String getRealm() {
          return null;
        }

        @Override
        public boolean authenticate(String username, String password, HttpRequest request) {
          return USERNAME.equals(username) && PASSWORD.equals(password);
        }
    }
}
