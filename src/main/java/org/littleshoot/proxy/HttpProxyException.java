package org.littleshoot.proxy;

public class HttpProxyException extends RuntimeException {

  public HttpProxyException(String details, Throwable e) {
    super(e.getMessage() + ", details: " + details, e);
  }
}
