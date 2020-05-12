package io.netty.handler.codec.compression;

import static io.netty.handler.codec.compression.BrotliHttpContentDecompressor.BR;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentEncoder;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;

/**
 * Compresses an {@link HttpMessage} and an {@link HttpContent} in {@code brotli} encoding while respecting the {@code
 * "Accept-Encoding"} header. If there is no matching encoding, no compression is done.  For more information on how
 * this handler modifies the message, please refer to {@link HttpContentEncoder}.
 */
public class BrotliHttpContentCompressor extends HttpContentEncoder {

  private ChannelHandlerContext ctx;

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    this.ctx = ctx;
  }

  @Override
  protected Result beginEncode(HttpResponse headers, String acceptEncoding) throws Exception {

    String contentEncoding = headers.headers().get(HttpHeaderNames.CONTENT_ENCODING);
    if (contentEncoding != null) {
      // Content-Encoding was set, either as something specific or as the IDENTITY encoding
      // Therefore, we should NOT encode here
      return null;
    }
    Object preferencesConfig = ctx.channel().attr(AttributeKey.valueOf("preferences")).get();

    if (BR.contentEqualsIgnoreCase(acceptEncoding)) {
      return new Result(
          acceptEncoding,
          new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
              ctx.channel().config(), new BrotliEncoder()));
    }
    // 'identity' or unsupported
    return null;
  }
}
