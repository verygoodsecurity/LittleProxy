package io.netty.handler.codec.compression;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentEncoder;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AsciiString;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;

/**
 * Compresses an {@link HttpMessage} and an {@link HttpContent} in {@code brotli} encoding while respecting the {@code
 * "Accept-Encoding"} header. If there is no matching encoding, no compression is done.  For more information on how
 * this handler modifies the message, please refer to {@link HttpContentEncoder}.
 */
public class BrotliHttpContentCompressor extends HttpContentEncoder {

  private ChannelHandlerContext ctx;

  public static final AttributeKey<String> CONTENT_COMPRESSION_ATTRIBUTE = AttributeKey.valueOf("contentCompression");

  public static final AsciiString BR = AsciiString.cached("br");

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    this.ctx = ctx;
  }

  @Override
  protected Result beginEncode(HttpResponse response, String acceptEncoding) throws Exception {

    String contentEncoding = response.headers().get(HttpHeaderNames.CONTENT_ENCODING);
    if (contentEncoding != null) {
      // Content-Encoding was set, either as something specific or as the IDENTITY encoding
      // Therefore, we should NOT encode here
      return null;
    }

    if (acceptEncoding.contains(BR) && brotliCompressionEnabled()) {
      return new Result(
          acceptEncoding,
          new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
              ctx.channel().config(), new BrotliEncoder()));
    }
    // 'identity' or unsupported
    return null;
  }

  private boolean brotliCompressionEnabled() {
    String compressionAttribute = ctx.channel().attr(CONTENT_COMPRESSION_ATTRIBUTE).get();
    if (StringUtils.isEmpty(compressionAttribute)) {
      return false;
    }
    return compressionAttribute.contains(BR.toString());
  }
}
