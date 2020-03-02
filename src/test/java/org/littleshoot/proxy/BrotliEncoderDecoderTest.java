package org.littleshoot.proxy;

import com.google.common.io.Resources;

import org.junit.Test;

import java.io.IOException;

import io.netty.handler.codec.compression.BrotliDecoder;

import static org.junit.Assert.assertArrayEquals;

public class BrotliEncoderDecoderTest extends AbstractProxyTest {
    @Override
    protected void setUp() {
        this.proxyServer = bootstrapProxy()
                .withPort(0)
                .start();
    }
  private static byte[] loadUncompressedSample() throws IOException {
     return Resources.toByteArray(BrotliEncoderDecoderTest.class.getResource("/brotli/a100.txt"));
   }

   private static byte[] loadBrotliCompressedSample() throws IOException {
     return Resources.toByteArray(BrotliEncoderDecoderTest.class.getResource("/brotli/a100.txt.br"));
   }

  @Test
  public void decompressBrotliContents() throws IOException {
    byte[] brotliBytes = loadBrotliCompressedSample();
    byte[] decompressedBytes = BrotliDecoder.decompress(brotliBytes);
    byte[] expected = loadUncompressedSample();
    assertArrayEquals("bytes", expected, decompressedBytes);
  }

}