package org.littleshoot.proxy;

import com.google.common.io.Resources;

import org.apache.http.HttpHost;
import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import io.netty.handler.codec.compression.BrotliDecoder;

import static org.junit.Assert.assertArrayEquals;

public class BrotliEncoderDecoderTest extends AbstractProxyTest {

  private Server webServer;

  @Before
  @Override
  public void runSetUp() throws Exception {
    System.out.println("Got here!");
    webServer = TestUtils.startWebServer(true);

    // find out what ports the HTTP and HTTPS connectors were bound to
    httpsWebServerPort = TestUtils.findLocalHttpsPort(webServer);
    if (httpsWebServerPort < 0) {
      throw new RuntimeException("HTTPS connector should already be open and listening, but port was " + webServerPort);
    }

    webServerPort = TestUtils.findLocalHttpPort(webServer);
    if (webServerPort < 0) {
      throw new RuntimeException("HTTP connector should already be open and listening, but port was " + webServerPort);
    }

    webHost = new HttpHost("127.0.0.1", webServerPort);
    httpsWebHost = new HttpHost("127.0.0.1", httpsWebServerPort, "https");

    setUp();
  }

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