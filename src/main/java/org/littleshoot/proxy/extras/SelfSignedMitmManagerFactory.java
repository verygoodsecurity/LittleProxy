package org.littleshoot.proxy.extras;

import io.netty.channel.Channel;
import org.littleshoot.proxy.MitmManager;
import org.littleshoot.proxy.MitmManagerFactory;

/**
 * The factory for self signed mitm manager
 */
public class SelfSignedMitmManagerFactory implements MitmManagerFactory {
  @Override
  public MitmManager getInstance(Channel channel) {
    return new SelfSignedMitmManager();
  }
}
