package org.littleshoot.proxy;

import io.netty.channel.Channel;

public interface MitmManagerFactory {

  /**
   * Retrieves an instance of {@link MitmManager} based on specific attributes from cxt channel
   *
   * @param channel current channel from cxt
   * @return concrete instance of {@link MitmManager}
   */
  MitmManager getInstance(Channel channel);
}
