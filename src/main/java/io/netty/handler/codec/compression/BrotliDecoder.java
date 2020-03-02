/*
 * Copyright 2013 The Netty Project
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
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Decompress a {@link ByteBuf} using the inflate algorithm.
 */
public class BrotliDecoder extends ByteToMessageDecoder {

  private static final InternalLogger log =
      InternalLoggerFactory.getInstance(BrotliDecoder.class);

  static {
    BrotliLoader.isBrotliAvailable();
  }

  /*
  For how this value is derived, please see: `BROTLI_MAX_NUMBER_OF_BLOCK_TYPES` in these docs:
     - https://github.com/google/brotli/blob/master/c/common/constants.h
     - https://tools.ietf.org/html/draft-vandevenne-shared-brotli-format-01
   */
  private static int BROTLI_MAX_NUMBER_OF_BLOCK_TYPES = 256;

  public BrotliDecoder() {
  }

  public static byte[] decompress(byte[] compressedArray) throws IOException {
    if(compressedArray == null) {
      return null;
    }

    ByteArrayOutputStream out;
    try (BrotliInputStream is = new BrotliInputStream(new ByteArrayInputStream(compressedArray))) {
      out = new ByteArrayOutputStream();
      if (!decompress(out, is)) {
        return null;
      }
    } catch (IOException e) {
      log.error("Unhandled exception when decompressing brotli", e);
      throw e;
    }
    return out.toByteArray();
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    /*
       use in.alloc().buffer() instead of Unpooled.buffer() as best practice.
       See: https://github.com/netty/netty/wiki/New-and-noteworthy-in-4.0#pooled-buffers
    */
    try (ByteBufOutputStream output = new ByteBufOutputStream(in.alloc().buffer())) {
      try (BrotliInputStream brotliInputStream = new BrotliInputStream(new ByteBufInputStream(in))) {
        if (!decompress(output, brotliInputStream)) return;
      } catch (IOException e) {
        log.error("Unhandled exception when decompressing brotli", e);
        throw e;
      }
      out.add(output.buffer());
    }
  }

  private static boolean decompress(OutputStream output, BrotliInputStream brotliInputStream) throws IOException {
    byte[] decompressBuffer = new byte[BROTLI_MAX_NUMBER_OF_BLOCK_TYPES];
    // is the stream ready for us to decompress?
    int bytesRead;
    try {
      bytesRead = brotliInputStream.read(decompressBuffer);
    } catch (IOException e) {
      // unexpected end of input, not ready to decompress, so just return
      return false;
    }
    // continue reading until we have hit EOF
    while (bytesRead > -1) { // -1 means EOF
      output.write(decompressBuffer, 0, bytesRead);
      Arrays.fill(decompressBuffer, (byte) 0);
      bytesRead = brotliInputStream.read(decompressBuffer);
    }
    return true;
  }
}