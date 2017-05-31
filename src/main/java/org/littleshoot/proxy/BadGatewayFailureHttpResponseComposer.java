package org.littleshoot.proxy;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.littleshoot.proxy.impl.ProxyUtils;


public final class BadGatewayFailureHttpResponseComposer implements ServerConnectionFailureHttpResponseComposer {

  /**
   * Tells the client that something went wrong trying to proxy its request. If the Bad Gateway is a response to
   * an HTTP HEAD request, the response will contain no body, but the Content-Length header will be set to the
   * value it would have been if this 502 Bad Gateway were in response to a GET.
   *
   * @param httpRequest the HttpRequest that is resulting in the Bad Gateway response
   * @param cause raised exception
   * @return true if the connection will be kept open, or false if it will be disconnected
   */
  @Override
  public FullHttpResponse compose(HttpRequest httpRequest, Throwable cause) {
    String body = "Bad Gateway: " + httpRequest.getUri();

    FullHttpResponse response = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, body);

    if (ProxyUtils.isHEAD(httpRequest)) {
      // don't allow any body content in response to a HEAD request
      response.content().clear();
    }
    return response;
  }

  public FullHttpResponse compose(HttpRequest httpRequest) {
    return this.compose(httpRequest, null);
  }
}
