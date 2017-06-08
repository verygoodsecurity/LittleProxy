package org.littleshoot.proxy;

import io.netty.channel.Channel;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;

/**
 * Tests a single proxy that requires username/password authentication and that
 * uses MITM.
 */
public class MITMUsernamePasswordAuthenticatingProxyTest extends
        UsernamePasswordAuthenticatingProxyTest
        implements ProxyAuthenticator {
    @Override
    protected void setUp() {
        this.proxyServer = bootstrapProxy()
                .withPort(0)
                .withProxyAuthenticator(this)
                .withManInTheMiddle(new SelfSignedMitmManagerFactory())
                .start();
    }

    @Override
    protected boolean isMITM() {
        return true;
    }
}
