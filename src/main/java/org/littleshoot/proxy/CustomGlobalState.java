package org.littleshoot.proxy;

import io.netty.channel.Channel;

public interface CustomGlobalState {

  /**
   * A complete proxy request is composed of different channels which
   * can be handled by different threads. In case a thread local is used
   * for storing global state (or similar) this architecture can cause problems.
   *
   * This method is invoked before prior to work with channel in the same thread
   * so it lets restore the state based on client connection channel attributes
   * @param channel client connection channel
   */
  void restore(Channel channel);

}
