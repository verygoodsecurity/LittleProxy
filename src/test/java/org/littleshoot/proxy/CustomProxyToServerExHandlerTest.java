package org.littleshoot.proxy;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.littleshoot.proxy.extras.SelfSignedMitmManagerFactory;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.util.ArrayList;
import java.util.List;

public class CustomProxyToServerExHandlerTest extends MitmWithBadServerAuthenticationTCPChainedProxyTest {

  private final List<Throwable> customExHandlerEntered = new ArrayList<>();

  @Override
  protected void setUp() {
    this.upstreamProxy = upstreamProxy().start();

    this.proxyServer = bootstrapProxy()
        .withPort(0)
        .withChainProxyManager(chainedProxyManager())
        .plusActivityTracker(DOWNSTREAM_TRACKER)
        .withManInTheMiddle(new SelfSignedMitmManagerFactory())
        .withProxyToServerExHandler(customExHandlerEntered::add)
        .start();
  }

  @Override
  protected void tearDown() throws Exception {
    this.upstreamProxy.abort();
  }

  @Test
  @Ignore // this test is flack, needs to be fixed
  public void testCustomProxyToServerExHandler() throws Exception {
    System.out.println("==========1" + proxyServer.toString());
    System.out.println("==========2" + ((DefaultHttpProxyServer)proxyServer).getProxyToServerExHandler());
    super.testSimpleGetRequestOverHTTPS();
    System.out.println(customExHandlerEntered);
    Assert.assertFalse("Custom ex handler was not called", customExHandlerEntered.isEmpty());
    Assert.assertEquals("Incorrect exception was passed to custom ex handles",
        customExHandlerEntered.get(0).getMessage(), "javax.net.ssl.SSLHandshakeException: General SSLEngine problem");
  }
}
