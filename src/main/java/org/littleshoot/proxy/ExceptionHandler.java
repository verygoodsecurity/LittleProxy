package org.littleshoot.proxy;

public interface ExceptionHandler {

  /**
   * Handles proxy exceptions
   *
   * @param cause error cause
   */
  void handle(Throwable cause);
}
