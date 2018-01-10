package org.littleshoot.proxy;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Interface for objects that can provide a custom http response on a specific failure.
 */
public interface FailureHttpResponseComposer {

  /**
   * Creates an {@link FullHttpResponse} based on initial request and failure cause
   * @param httpRequest initial request
   * @param cause an exception thrown during a failure
   * @return failure http response
   */
  FullHttpResponse compose(HttpRequest httpRequest, Throwable cause);
}
