package io.netty.handler.codec.compression;

import static io.netty.handler.codec.compression.BrotliHttpContentCompressor.BR;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecoder;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpMessage;

/**
 * Decompresses an {@link HttpMessage} and an {@link HttpContent} compressed in {@code br}, {@code gzip} or
 * {@code deflate} encoding. For more information on how this handler modifies the message, please refer to
 * {@link HttpContentDecoder}.
 */
public class GeneralHttpContentDecompressor extends HttpContentDecompressor {

  @Override
  protected EmbeddedChannel newContentDecoder(String contentEncoding) throws Exception {

    if (BR.contentEqualsIgnoreCase(contentEncoding)) {
      return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
          ctx.channel().config(), new BrotliDecoder());
    }

    return super.newContentDecoder(contentEncoding);
  }
}
