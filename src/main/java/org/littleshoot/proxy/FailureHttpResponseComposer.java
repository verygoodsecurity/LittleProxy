package org.littleshoot.proxy;


import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

public interface FailureHttpResponseComposer {
  FullHttpResponse compose(HttpRequest httpRequest, Throwable cause);
}
