package org.littleshoot.proxy.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.ReferenceCountUtil;

public class UpstreamConnectionHandler extends ChannelInboundHandlerAdapter {

  private final ClientToProxyConnection clientToProxyConnection;

  UpstreamConnectionHandler(ClientToProxyConnection clientToProxyConnection) {
    this.clientToProxyConnection = clientToProxyConnection;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object request) {
    try {
      final ConnectionState connectionState =
          clientToProxyConnection.setupUpstreamConnection(((Request) request).getShortCircuitResponse(),
              ((Request) request).getInitialRequest());
      clientToProxyConnection.become(connectionState);
    } finally {
      ReferenceCountUtil.release(((Request) request).getInitialRequest());
    }
  }

  @Override
  public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    clientToProxyConnection.exceptionCaught(cause);
  }

  public static class Request {
    private final HttpRequest initialRequest;
    private final HttpResponse shortCircuitResponse;

    public Request(HttpRequest initialRequest, HttpResponse shortCircuitResponse) {
      this.initialRequest = initialRequest;
      this.shortCircuitResponse = shortCircuitResponse;
    }

    HttpRequest getInitialRequest() { return initialRequest; }
    HttpResponse getShortCircuitResponse() { return shortCircuitResponse; }
  }
}
