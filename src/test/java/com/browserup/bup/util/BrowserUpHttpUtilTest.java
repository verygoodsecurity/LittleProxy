package com.browserup.bup.util;

import com.google.common.io.Resources;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class BrowserUpHttpUtilTest {

  @Test
  public void decompressBrotliContents() throws IOException {
    byte[] brotliBytes = loadBrotliCompressedSample();
    byte[] decompressedBytes = BrowserUpHttpUtil.decompressBrotliContents(brotliBytes);
    byte[] expected = loadUncompressedSample();
    assertArrayEquals("bytes", expected, decompressedBytes);
  }

  private static byte[] loadUncompressedSample() throws IOException {
    return Resources.toByteArray(BrowserUpHttpUtilTest.class.getResource("/brotli/a100.txt"));
  }

  private static byte[] loadBrotliCompressedSample() throws IOException {
    return Resources.toByteArray(BrowserUpHttpUtilTest.class.getResource("/brotli/a100.txt.br"));
  }
}