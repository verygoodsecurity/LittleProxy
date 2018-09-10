package org.littleshoot.proxy.impl;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.util.AttributeKey;

import static io.netty.handler.codec.http.HttpResponseStatus.SWITCHING_PROTOCOLS;

/**
 * Handle http upgrade(websocket, http2).
 * Note: http2 client may send preface frame directly, with no upgrade request.
 */
public class HttpUpgradeHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpUpgradeHandler.class);

    // the current http request/response
    private boolean upgradeWebSocket;
    private boolean upgradeWebSocketSucceed;

    private final ChannelPipeline proxyToServerPipeline;

    public HttpUpgradeHandler(ChannelPipeline proxyToServerPipeline) {
        this.proxyToServerPipeline = proxyToServerPipeline;
    }

    // read request
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof HttpMessage)) {
            logger.debug("not http message: {}", msg.getClass().getName());
            ctx.fireChannelRead(msg);
            return;
        }

        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            HttpHeaders headers = request.headers();
            Collection<String> items = getHeaderValues(Strings.nullToEmpty(headers.get("Connection")));
            if (items.contains("upgrade")) {
                String upgrade = Strings.nullToEmpty(headers.get("Upgrade")).trim().toLowerCase();
                switch (upgrade) {
                    case "websocket":
                        upgradeWebSocket = true;
                        ctx.channel().attr(AttributeKey.<Boolean>valueOf("ws-protocol")).set(true);
                        break;
                    default:
                        logger.warn("unsupported upgrade header value: {}", upgrade);
                }
            }
        }

        if (msg instanceof HttpContent) {
            if (msg instanceof LastHttpContent) {
                if (!upgradeWebSocket) {
                    ctx.pipeline().remove(this);
                }
            }
        }
        ctx.fireChannelRead(msg);
    }

    // read response
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof HttpObject)) {
            logger.error("not http message: {}", msg.getClass().getName());
            ctx.write(msg, promise);
            return;
        }

        if (msg instanceof HttpResponse) {
            // either upgradeWebSocket or upgradeH2c should be true
            HttpResponse response = (HttpResponse) msg;
            if (upgradeWebSocket) {
                upgradeWebSocketSucceed = webSocketUpgraded(response);
                if (!upgradeWebSocketSucceed) {
                    ctx.write(msg, promise);
                    ctx.pipeline().remove(this);
                    return;
                }
            } else {
                // when write request should have remove this handler
                logger.debug("no upgrade found but get a response?");
            }
        }

        if (msg instanceof HttpContent) {
            if (msg instanceof LastHttpContent) {
                ctx.write(msg, promise);
                ctx.pipeline().remove(this);
                if (upgradeWebSocketSucceed) {
                    upgradeWebSocket(ctx);
                }
                return;
            }
        }

        ctx.write(msg, promise);
    }

    private void upgradeWebSocket(ChannelHandlerContext ctx) {
        logger.debug("upgrade to web-socket");
//        ctx.pipeline().replace("http-interceptor", "ws-interceptor",
//                new WebSocketInterceptor(address.host(), wsUrl, messageListener));

        WebSocketFrameDecoder frameDecoder = new WebSocket13FrameDecoder(true, true, 65536, false);
        WebSocketFrameEncoder frameEncoder = new WebSocket13FrameEncoder(false);
        ctx.pipeline().addBefore("decoder", "ws-decoder", frameDecoder);
        ctx.pipeline().addBefore("encoder", "ws-encoder", frameEncoder);

        ChannelPipeline pipeline = ctx.pipeline();
        if (pipeline.get("encoder") != null) {
            pipeline.remove("encoder");
        }
        if (pipeline.get("responseWrittenMonitor") != null) {
            pipeline.remove("responseWrittenMonitor");
        }
        if (pipeline.get("decoder") != null) {
            pipeline.remove("decoder");
        }
        if (pipeline.get("requestReadMonitor") != null) {
            pipeline.remove("requestReadMonitor");
        }

        ctx.channel().attr(AttributeKey.<Boolean>valueOf("tunneling")).set(true);

        WebSocketFrameDecoder clientFrameDecoder = new WebSocket13FrameDecoder(false, true, 65536, false);
        WebSocketFrameEncoder clientFrameEncoder = new WebSocket13FrameEncoder(true);
        proxyToServerPipeline.addBefore("decoder", "ws-decoder", clientFrameDecoder);
        proxyToServerPipeline.addBefore("encoder", "ws-encoder", clientFrameEncoder);

        if (proxyToServerPipeline.get("encoder") != null) {
            proxyToServerPipeline.remove("encoder");
        }
        if (proxyToServerPipeline.get("responseWrittenMonitor") != null) {
            proxyToServerPipeline.remove("responseWrittenMonitor");
        }
        if (proxyToServerPipeline.get("decoder") != null) {
            proxyToServerPipeline.remove("decoder");
        }
        if (proxyToServerPipeline.get("requestReadMonitor") != null) {
            proxyToServerPipeline.remove("requestReadMonitor");
        }

        proxyToServerPipeline.channel().attr(AttributeKey.<Boolean>valueOf("tunneling")).set(true);

    }

    private Collection<String> getHeaderValues(String value) {
        return Stream.of(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    private boolean webSocketUpgraded(HttpResponse response) {
        return checkUpgradeResponse(response, "websocket");
    }

    private boolean checkUpgradeResponse(HttpResponse response, String protocol) {
        HttpHeaders headers = response.headers();
        if (!response.status().equals(SWITCHING_PROTOCOLS)) {
            return false;
        }
        String connectionHeader = Strings.nullToEmpty(headers.get("Connection"));
        String upgradeHeader = Strings.nullToEmpty(headers.get("Upgrade"));
        return connectionHeader.equals("Upgrade") && upgradeHeader.equalsIgnoreCase(protocol);
    }

}