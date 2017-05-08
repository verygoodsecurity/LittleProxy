package org.littleshoot.proxy;

import com.google.common.base.Joiner;

public class HttpProxyException extends RuntimeException {

  public HttpProxyException(RuntimeException e, String ... details) {
    super(e.getMessage() + ", details: " + Joiner.on(", ").join(details), e);
  }
}
