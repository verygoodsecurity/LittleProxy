package org.littleshoot.proxy.impl;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.Test;
import org.littleshoot.proxy.BadGatewayFailureHttpResponseComposer;
import org.littleshoot.proxy.ServerConnectionFailureHttpResponseComposer;

import static org.junit.Assert.assertTrue;

public class DefaultHttpProxyServerTest {

  @Test
  public void testDefaultUnrecoverableFailureHttpResponseComposer() {
    DefaultHttpProxyServer httpProxyServer = (DefaultHttpProxyServer) DefaultHttpProxyServer.bootstrap().start();
    assertTrue(httpProxyServer.getUnrecoverableFailureHttpResponseComposer() instanceof BadGatewayFailureHttpResponseComposer);
    httpProxyServer.stop();
  }

  @Test
  public void testCustomUnrecoverableFailureHttpResponseComposer() {

    class CustomUnrecoverableFailureHttpResponseComposer implements ServerConnectionFailureHttpResponseComposer {
      @Override
      public FullHttpResponse compose(HttpRequest httpRequest, Throwable cause) {
        return null;
      }
    }

    DefaultHttpProxyServer httpProxyServer = (DefaultHttpProxyServer) DefaultHttpProxyServer
        .bootstrap()
        .withUnrecoverableFailureHttpResponseComposer(new CustomUnrecoverableFailureHttpResponseComposer())
        .start();
    assertTrue(httpProxyServer.getUnrecoverableFailureHttpResponseComposer() instanceof CustomUnrecoverableFailureHttpResponseComposer);
    httpProxyServer.stop();
  }

}