package org.littleshoot.proxy;

import io.netty.channel.Channel;

public interface RequestTracer {

  /**
   * Start tracing proxy request
   * @param channel
   */
  void start(Channel channel);

  /**
   * Request is served. Finish tracing.
   * @param channel
   */
  void finish(Channel channel);
}
