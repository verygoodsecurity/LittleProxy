package org.littleshoot.proxy;

import io.netty.channel.Channel;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;

/**
 * The factory for self signed mitm manager
 */
public class SelfSignedMitmManagerFactory implements MitmManagerFactory {
  @Override
  public MitmManager getInstance(Channel channel) {
    return new SelfSignedMitmManager();
  }
}
