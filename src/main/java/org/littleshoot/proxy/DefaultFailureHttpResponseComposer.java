package org.littleshoot.proxy;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.littleshoot.proxy.impl.ProxyUtils;

public class DefaultFailureHttpResponseComposer implements FailureHttpResponseComposer {

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
    String body = provideCustomMessage(httpRequest, cause);
    HttpResponseStatus status = provideCustomStatus(httpRequest, cause);

    FullHttpResponse response = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_1, status, body);

    if (ProxyUtils.isHEAD(httpRequest)) {
      // don't allow any body content in response to a HEAD request
      response.content().clear();
    }
    return response;
  }

  /**
   * The method can be overridden to provide a custom message along with 502 code
   * @param httpRequest initial request
   * @param cause an exception thrown on a failure
   * @return custom message
   */
  protected String provideCustomMessage(HttpRequest httpRequest, Throwable cause) {
    return "Bad Gateway: " + httpRequest.getUri();
  }

  protected HttpResponseStatus provideCustomStatus(HttpRequest httpRequest, Throwable cause) {
    return HttpResponseStatus.BAD_GATEWAY;
  }
}
