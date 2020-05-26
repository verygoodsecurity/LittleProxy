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

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import com.nixxcode.jvmbrotli.enc.Encoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BrotliEncoder extends MessageToByteEncoder<ByteBuf> {

  private static final Logger log = LoggerFactory.getLogger(BrotliEncoder.class);

  private final boolean preferDirect;
  private final int compressionQuality;
  private final int windowSize;

  /*
   If the Brotli encoding is being used to compress streams in real-time,
   it is not advisable to have a quality setting above 4 due to performance.
  */
  private static final int DEFAULT_COMPRESSION_QUALITY = 4;

  private static final int DEFAULT_WINDOW_SIZE = -1;


  public BrotliEncoder() {
    this(true, DEFAULT_COMPRESSION_QUALITY, DEFAULT_WINDOW_SIZE);
  }

  public BrotliEncoder(int quality) {
    this(true, quality, DEFAULT_WINDOW_SIZE);
  }

  public BrotliEncoder(int quality, int windowSize) {
    this(true, quality, windowSize);
  }

  public BrotliEncoder(boolean preferDirect) {
    this(preferDirect, DEFAULT_COMPRESSION_QUALITY, DEFAULT_WINDOW_SIZE);
  }

  public BrotliEncoder(boolean preferDirect, int compressionQuality, int windowSize) {
    super(preferDirect);
    this.preferDirect = preferDirect;
    this.compressionQuality = compressionQuality;
    this.windowSize = windowSize;
    // https://github.com/nixxcode/jvm-brotli#loading-jvm-brotli
    if (!BrotliLoader.isBrotliAvailable()) {
      throw new CompressionException("Brotli encoding is not supported");
    }
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf uncompressed, ByteBuf out) throws Exception {
    if (!uncompressed.isReadable() || uncompressed.readableBytes() == 0) {
      return;
    }
    try (ByteBufOutputStream dst = new ByteBufOutputStream(uncompressed.alloc().buffer())) {
      Encoder.Parameters params = new Encoder.Parameters().setQuality(this.compressionQuality);
      try (BrotliOutputStream brotliOutputStream = new BrotliOutputStream(dst, params)) {
        brotliOutputStream
            .write(ByteBufUtil.getBytes(uncompressed, uncompressed.readerIndex(), uncompressed.readableBytes(), false));
        brotliOutputStream.flush();
      }
      out.writeBytes(dst.buffer());
    }
  }

  @Override
  public boolean isPreferDirect() {
    return preferDirect;
  }
}
