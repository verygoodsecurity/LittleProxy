package org.littleshoot.proxy;

import org.littleshoot.proxy.extras.SelfSignedMitmManagerFactory;

/**
 * Tests a single proxy that requires username/password authentication and that
 * uses MITM.
 */
public class MITMUsernamePasswordAuthenticatingProxyTest extends
        UsernamePasswordAuthenticatingProxyTest {
    @Override
    protected void setUp() {
        this.proxyServer = bootstrapProxy()
                .withPort(0)
                .withProxyAuthenticator(new TestBasicProxyAuthenticator())
                .withManInTheMiddle(new SelfSignedMitmManagerFactory())
                .start();
    }

    @Override
    protected boolean isMITM() {
        return true;
    }
}
