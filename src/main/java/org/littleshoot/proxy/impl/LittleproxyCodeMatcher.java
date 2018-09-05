package org.littleshoot.proxy.impl;

import net.dongliu.proxy.netty.detector.ProtocolMatcher;

import org.littleshoot.proxy.SslEngineSource;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

public class LittleproxyCodeMatcher extends ProtocolMatcher {

  DefaultHttpProxyServer server;
  SslEngineSource sslEngineSource;
  boolean authenticateSslClients;
  GlobalTrafficShapingHandler globalTrafficShapingHandler;


  LittleproxyCodeMatcher(DefaultHttpProxyServer server, SslEngineSource sslEngineSource,
                         boolean authenticateSslClients, GlobalTrafficShapingHandler globalTrafficShapingHandler) {
    this.server = server;
    this.sslEngineSource = sslEngineSource;
    this.authenticateSslClients = authenticateSslClients;
    this.globalTrafficShapingHandler = globalTrafficShapingHandler;
  }

  @Override
  protected int match(ByteBuf byteBuf) {
    return 1;
  }

  @Override
  public void handlePipeline(ChannelPipeline pipeline) {
    new ClientToProxyConnection(
        server,
        sslEngineSource,
        authenticateSslClients,
        pipeline,
        globalTrafficShapingHandler);
  }
}
