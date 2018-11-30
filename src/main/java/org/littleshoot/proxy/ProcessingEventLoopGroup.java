package org.littleshoot.proxy;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

public interface ProcessingEventLoopGroup {
  EventLoopGroup forChannel(Channel channel);
}
