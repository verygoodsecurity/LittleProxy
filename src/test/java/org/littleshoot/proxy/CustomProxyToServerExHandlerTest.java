package org.littleshoot.proxy;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.littleshoot.proxy.extras.SelfSignedMitmManagerFactory;

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
        .withProxyToServerExHandler(new ExceptionHandler() {
          @Override
          public void handle(Throwable cause) {
            customExHandlerEntered.add(cause);
          }
        })
        .start();
  }

  @Override
  protected void tearDown() throws Exception {
    this.upstreamProxy.abort();
  }

  @Ignore
  @Test
  public void testCustomProxyToServerExHandler() throws Exception {
    super.testSimpleGetRequestOverHTTPS();
    Assert.assertFalse("Custom ex handler was not called", customExHandlerEntered.isEmpty());
    Assert.assertEquals("Incorrect exception was passed to custom ex handles",
        customExHandlerEntered.get(0).getMessage(), "javax.net.ssl.SSLHandshakeException: General SSLEngine problem");
  }
}
