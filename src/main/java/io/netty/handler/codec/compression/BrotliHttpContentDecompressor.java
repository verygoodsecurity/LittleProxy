/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.compression;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecoder;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.util.AsciiString;
import io.netty.util.AttributeKey;

/**
 * Decompresses an {@link HttpMessage} and an {@link HttpContent} compressed in {@code br}. For more information on how
 * this handler modifies the message, please refer to {@link HttpContentDecoder}.
 */
public class BrotliHttpContentDecompressor extends HttpContentDecoder {

  public static final AsciiString BR = AsciiString.cached("br");

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    this.ctx = ctx;
  }

  @Override
  protected EmbeddedChannel newContentDecoder(String contentEncoding) throws Exception {
    Object preferencesConfig = ctx.channel().attr(AttributeKey.valueOf("preferences")).get();

    if (BR.contentEqualsIgnoreCase(contentEncoding)) {
      return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
          ctx.channel().config(), new BrotliDecoder());
    }
    // 'identity' or unsupported
    return null;
  }
}
