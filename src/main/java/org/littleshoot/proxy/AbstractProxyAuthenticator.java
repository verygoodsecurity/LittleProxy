package org.littleshoot.proxy;

import org.littleshoot.proxy.impl.ProxyUtils;

import java.util.Date;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public abstract class AbstractProxyAuthenticator implements ProxyAuthenticator {

  @Override
  public abstract boolean authenticate(HttpRequest request);

  @Override
  public abstract String getRealm();

  @Override
  public FullHttpResponse authenticationFailureResponse(HttpRequest request) {
    String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
        + "<html><head>\n"
        + "<title>407 Proxy Authentication Required</title>\n"
        + "</head><body>\n"
        + "<h1>Proxy Authentication Required</h1>\n"
        + "<p>This server could not verify that you\n"
        + "are authorized to access the document\n"
        + "requested.  Either you supplied the wrong\n"
        + "credentials (e.g., bad password), or your\n"
        + "browser doesn't understand how to supply\n"
        + "the credentials required.</p>\n" + "</body></html>\n";
    FullHttpResponse response = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_1,
        HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED, body);
    HttpHeaders.setDate(response, new Date());
    response.headers().set("Proxy-Authenticate", "Basic realm=\"" + (getRealm() == null ? "Restricted Files" : getRealm()) + "\"");
    return response;
  }
}
