package org.littleshoot.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.junit.Assert;
import org.junit.Test;
import org.littleshoot.proxy.extras.SelfSignedMitmManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class CustomClientToProxyExHandlerTest extends AbstractProxyTest {

  private final List<Throwable> customExHandlerEntered = new ArrayList<>();

  private static final String EXCEPTION_MESSAGE = "Error occurred in client to proxy connection";

  @Override
  protected void setUp() {
    this.proxyServer = bootstrapProxy()
        .withPort(0)
        .withManInTheMiddle(new SelfSignedMitmManagerFactory())
        .withClientToProxyExHandler(new ExceptionHandler() {
          @Override
          public void handle(Throwable cause) {
            customExHandlerEntered.add(cause);
          }
        })
        .withFiltersSource(new HttpFiltersSourceAdapter() {
          @Override
          public HttpFilters filterRequest(HttpRequest originalRequest,
                                           ChannelHandlerContext ctx) {
            throw new RuntimeException(EXCEPTION_MESSAGE);
          }
        })
        .start();
  }

  @Test
  public void testCustomClientToProxyExHandler() throws Exception {
    try {
      httpGetWithApacheClient(webHost, DEFAULT_RESOURCE, true, true);
    } catch (NoHttpResponseException e) {
      // expected
    }
    Assert.assertFalse("Custom ex handler was not called", customExHandlerEntered.isEmpty());
    Assert.assertEquals("Incorrect exception was passed to custom ex handles",
        customExHandlerEntered.get(0).getMessage(), EXCEPTION_MESSAGE);
  }
}
