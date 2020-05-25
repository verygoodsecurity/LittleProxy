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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decompress a {@link ByteBuf} using the inflate algorithm.
 */
public class BrotliDecoder extends ByteToMessageDecoder {

  private static final Logger log = LoggerFactory.getLogger(BrotliDecoder.class);

  /*
  For how this value is derived, please see: `BROTLI_MAX_NUMBER_OF_BLOCK_TYPES` in these docs:
     - https://github.com/google/brotli/blob/master/c/common/constants.h
     - https://tools.ietf.org/html/draft-vandevenne-shared-brotli-format-01
   */
  private static int BROTLI_MAX_NUMBER_OF_BLOCK_TYPES = 256;

  public BrotliDecoder() {
    // https://github.com/nixxcode/jvm-brotli#loading-jvm-brotli
    if (!BrotliLoader.isBrotliAvailable()) {
      throw new DecompressionException("Brotli decoding is not supported");
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    /*
       use in.alloc().buffer() instead of Unpooled.buffer() as best practice.
       See: https://github.com/netty/netty/wiki/New-and-noteworthy-in-4.0#pooled-buffers
    */
    boolean success = true;
    ByteBuf outBuffer = null;
    try (ByteBufOutputStream output = new ByteBufOutputStream(in.alloc().buffer())) {
      try (ByteBufInputStream bbin = new ByteBufInputStream(in)) {
        try (BrotliInputStream brotliInputStream = new BrotliInputStream(bbin)) {
          success = decompress(output, brotliInputStream);
          if (!success) {
            return;
          }
        }
      }
      outBuffer = output.buffer();
      if (outBuffer.isReadable()) {
        out.add(outBuffer);
      } else {
        log.warn("Could not read Brotli output.");
        success = false;
      }
    } finally {
      if (!success) {
        if (outBuffer != null) {
          outBuffer.release();
        }
      }
    }
  }

  public static boolean decompress(OutputStream output, BrotliInputStream brotliInputStream) {
    byte[] decompressBuffer = new byte[BROTLI_MAX_NUMBER_OF_BLOCK_TYPES];
    // is the stream ready for us to decompress?
    int bytesRead;
    try {
      bytesRead = brotliInputStream.read(decompressBuffer);
      // continue reading until we have hit EOF
      while (bytesRead > -1) { // -1 means EOF
        output.write(decompressBuffer, 0, bytesRead);
        Arrays.fill(decompressBuffer, (byte) 0);
        bytesRead = brotliInputStream.read(decompressBuffer);
      }
      return true;
    } catch (IOException e) {
      log.warn("Could not decompress a brotli stream.", e);
      // unexpected end of input, not ready to decompress, so just return
      return false;
    } catch (RuntimeException e) {
      log.warn("Could not decompress a brotli stream.", e);
      // unexpected runtime error
      return false;
    }
  }
}