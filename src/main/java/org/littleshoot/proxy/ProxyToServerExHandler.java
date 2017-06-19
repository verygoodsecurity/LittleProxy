package org.littleshoot.proxy;

public interface ProxyToServerExHandler {

  /**
   * Handles proxy to server error
   * @param cause error cause
   */
  void handle(Throwable cause);
}
