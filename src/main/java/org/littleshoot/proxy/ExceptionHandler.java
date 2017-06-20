package org.littleshoot.proxy;

public interface ExceptionHandler {

  /**
   * Handles proxy exception
   * 
   * @param cause error cause
   */
  void handle(Throwable cause);
}
