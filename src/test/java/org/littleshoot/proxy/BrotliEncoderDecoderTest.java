package org.littleshoot.proxy;

import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.Encoder;

import io.netty.handler.codec.compression.BrotliDecoder;
import java.io.ByteArrayOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.DecompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.littleshoot.proxy.extras.SelfSignedSslEngineSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.lang.Float.parseFloat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BrotliEncoderDecoderTest extends AbstractProxyTest {

  @BeforeClass
  public static void setUpClass() {
    BrotliLoader.isBrotliAvailable();
  }

  @Before
  @Override
  public void runSetUp() {
    webServer = startWebServer(true);
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

  private static final Logger log = LoggerFactory.getLogger(BrotliEncoderDecoderTest.class);

  static class BrotliDecompressingEntity extends DecompressingEntity {
    BrotliDecompressingEntity(HttpEntity entity) {
      super(entity, inputStream -> {
        byte[] isAsBytes = IOUtils.toByteArray(inputStream);
        byte[] decompress = decompressBytes(isAsBytes);
        return new ByteArrayInputStream(decompress);
      });
    }

    public static boolean usesBrotliContentEncoding(Header contentEncoding) {
      if (contentEncoding == null) return false;
      return Arrays.stream(contentEncoding.getElements())
          .anyMatch(
              codec -> BrotliHandler.BROTLI_HTTP_CONTENT_CODING.equalsIgnoreCase(codec.getName())
          );
    }
  }

  static class HttpAcceptEncodingParser {


    private static final String CODING_SEPARATOR = ",";
    private static final String CODING_QVALUE_SEPARATOR = ";";
    private static final String QVALUE_PREFIX = "q=";

    boolean acceptBrotliEncoding(HttpServletRequest httpRequest) {
      return acceptBrotliEncoding(httpRequest.getHeader(HttpHeaders.ACCEPT_ENCODING));
    }

    boolean acceptBrotliEncoding(String headerString) {
      if (null == headerString) {
        return false;
      }
      String[] weightedCodings = headerString.split(CODING_SEPARATOR, 0);

      for (String weightedCoding : weightedCodings) {
        String[] coding_and_qvalue = weightedCoding.trim().split(CODING_QVALUE_SEPARATOR, 2);

        if (coding_and_qvalue.length <= 0) {
          continue;
        }

        if (!BrotliHandler.BROTLI_HTTP_CONTENT_CODING.equals(coding_and_qvalue[0].trim())) {
          continue;
        }

        if (coding_and_qvalue.length == 1) {
          return true;
        } else {
          String qvalue = coding_and_qvalue[1].trim();
          if (!qvalue.startsWith(QVALUE_PREFIX)) {
            continue;
          }
          try {
            return parseFloat(qvalue.substring(2).trim()) > 0;
          } catch (NumberFormatException e) {
            return false;
          }
        }
      }
      return false;
    }
  }

  class BrotliHandler extends AbstractHandler {

    /**
     * As defined in RFC draft "Brotli Compressed Data Format"
     *
     * @see <a href="http://www.ietf.org/id/draft-alakuijala-brotli-08.txt"></a>
     */
    public static final String BROTLI_HTTP_CONTENT_CODING = "br";
    protected Encoder.Parameters params = new Encoder.Parameters().setQuality(1);
    private final HttpAcceptEncodingParser acceptEncodingParser = new HttpAcceptEncodingParser();


    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
      if (acceptEncodingParser.acceptBrotliEncoding(request)) {
        response.addHeader("Content-Encoding", BROTLI_HTTP_CONTENT_CODING);
        baseRequest.setHandled(true);
        response.setHeader(HttpHeaders.CONTENT_TYPE, "br");
        byte[] encodedBytes = loadBrotliCompressedSample();
        try (InputStream inputStream = ByteSource.wrap(encodedBytes).openStream()) {
          ByteStreams.copy(inputStream, response.getOutputStream());
        }
      }
    }

    @Override
    public void destroy() {

    }
  }

  private Server webServer;

  protected Server startWebServer(boolean enableHttps) {
    Server s = new Server(0);

    ContextHandlerCollection handlers = new ContextHandlerCollection();
    addHandler(handlers, "/brotli", new BrotliHandler());
    s.setHandler(handlers);
    if (enableHttps) s = enableHttpsForServer(s);

    try {
      s.start();
    } catch (Exception e) {
      throw new RuntimeException("Error starting Jetty web server", e);
    }
    return s;
  }

  private Server enableHttpsForServer(Server s) {
    // Add SSL connector
    org.eclipse.jetty.util.ssl.SslContextFactory sslContextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory();

    SelfSignedSslEngineSource contextSource = new SelfSignedSslEngineSource();
    SSLContext sslContext = contextSource.getSslContext();

    sslContextFactory.setSslContext(sslContext);
    SslSocketConnector connector = new SslSocketConnector(
        sslContextFactory);
    connector.setPort(0);
    /*
     * <p>Ox: For some reason, on OS X, a non-zero timeout can causes
     * sporadic issues. <a href="http://stackoverflow.com/questions
     * /16191236/tomcat-startup-fails
     * -due-to-java-net-socketexception-invalid-argument-on-mac-o">This
     * StackOverflow thread</a> has some insights into it, but I don't
     * quite get it.</p>
     *
     * <p>This can cause problems with Jetty's SSL handshaking, so I
     * have to set the handshake timeout and the maxIdleTime to 0 so
     * that the SSLSocket has an infinite timeout.</p>
     */
    connector.setHandshakeTimeout(0);
    connector.setMaxIdleTime(0);
    s.addConnector(connector);
    return s;
  }

  private void addHandler(ContextHandlerCollection handlers, String path, Handler handler) {
    ContextHandler contextHandler = new ContextHandler();
    contextHandler.setContextPath(path);
    contextHandler.setHandler(handler);
    handlers.addHandler(contextHandler);
  }


  private static byte[] loadUncompressedSample() throws IOException {
    return Resources.toByteArray(BrotliEncoderDecoderTest.class.getResource("/brotli/a100.txt"));
  }

  private static byte[] loadBrotliCompressedSample() throws IOException {
    return Resources.toByteArray(BrotliEncoderDecoderTest.class.getResource("/brotli/a100.txt.br"));
  }

  @Test
  public void testDecompressBrotliContents() throws IOException {
    byte[] brotliBytes = loadBrotliCompressedSample();
    byte[] decompressedBytes = decompressBytes(brotliBytes);
    byte[] expected = loadUncompressedSample();
    assertArrayEquals("bytes", expected, decompressedBytes);
  }

  @Test
  public void testGetBinaryFile() throws Exception {
    try (CloseableHttpClient httpclient = HttpClientBuilder
        .create()
        .addInterceptorFirst(this::supportBrotliEncoding)
        .build()) {
      HttpResponse response = runBrotliScenarioWithClient(httpclient);
      byte[] asBytes = EntityUtils.toByteArray(response.getEntity());
      assertArrayEquals(loadUncompressedSample(), decompressBytes(asBytes));
    }
  }

  @Test
  public void testGetWithBrotliAwareClient() throws Exception {
    try (CloseableHttpClient httpclient = HttpClientBuilder
        .create()
        .addInterceptorFirst(this::supportBrotliEncoding)
        .addInterceptorFirst(this::decompressOnResponse)
        .build()) {
      HttpResponse response = runBrotliScenarioWithClient(httpclient);
      String str = EntityUtils.toString(response.getEntity());
      assertArrayEquals(loadUncompressedSample(), str.getBytes());
    }
  }

  private void decompressOnResponse(HttpResponse response, HttpContext httpContext) {
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      Header contentEncoding = entity.getContentEncoding();
      if (BrotliDecompressingEntity.usesBrotliContentEncoding(contentEncoding)) {
        response.setEntity(new BrotliDecompressingEntity(response.getEntity()));
      }
    }
  }

  private void supportBrotliEncoding(HttpRequest request, HttpContext httpContext) {
    if (!request.containsHeader(HttpHeaders.ACCEPT_ENCODING)) {
      request.addHeader(
          HttpHeaders.ACCEPT_ENCODING,
          String.format("%s, gzip, deflate", BrotliHandler.BROTLI_HTTP_CONTENT_CODING)
      );
    }
  }

  private HttpResponse runBrotliScenarioWithClient(CloseableHttpClient httpclient) throws IOException, URISyntaxException {
    HttpEntity responseEntity;
    URL textFileUrl = new URL(webHost + "/brotli");
    HttpGet httpget = new HttpGet(textFileUrl.toURI());
    log.info("Executing request " + httpget.getRequestLine());
    CloseableHttpResponse response = httpclient.execute(httpget);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpURLConnection.HTTP_OK);
    responseEntity = response.getEntity();
    Assert.assertNotNull(responseEntity);
    Header contentEncoding = response.getFirstHeader(HttpHeaders.CONTENT_ENCODING);
    assertEquals(contentEncoding.getValue(), BrotliHandler.BROTLI_HTTP_CONTENT_CODING);
    return response;
  }

  @Test
  public void testGetWithProxySettings() throws IOException, URISyntaxException {
    // Note: we use 127.0.0.1 here because on OS X, using straight up
    // localhost yields a connect exception.
    final HttpHost proxyHost = new HttpHost(
        "127.0.0.1",
        this.proxyServer.getListenAddress().getPort(),
        "http");
    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    RequestConfig.Builder reqBuilder = RequestConfig.custom();
    init(clientBuilder, reqBuilder, proxyHost, null, true);
    try (CloseableHttpClient httpclient = clientBuilder.build()) {
      HttpResponse response = runBrotliScenarioWithClient(httpclient);
      String str = EntityUtils.toString(response.getEntity());
      assertArrayEquals(loadUncompressedSample(), str.getBytes());
    }
  }

  public void init(
      final HttpClientBuilder clientBuilder,
      final RequestConfig.Builder requestConfigBuilder,
      final HttpHost proxyHost,
      final CredentialsProvider proxyAuth,
      final boolean addInterceptors) {

    // Create default SSLContext
    final SSLContext sslcontext = SSLContexts.createDefault();

    // Setup SSL
    final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
        sslcontext,
        SSLConnectionSocketFactory.getDefaultHostnameVerifier()
    );

    // Setup client builder
    clientBuilder
        // disconnect requests after 30sec
        .setConnectionTimeToLive(30, TimeUnit.SECONDS)
        .setSSLSocketFactory(sslsf);

    if (addInterceptors) {
      clientBuilder
          .addInterceptorFirst(this::supportBrotliEncoding)
          .addInterceptorFirst(this::decompressOnResponse);
    }

    // Setup our RequestConfigBuilder

    // If we have a configured proxy host
    if (proxyHost != null) {
      // If we have proxy auth enabled
      if (proxyAuth != null) {
        // Attach Credentials provider to client builder.
        clientBuilder.setDefaultCredentialsProvider(proxyAuth);
      }
      // Attach Proxy to request config builder
      requestConfigBuilder.setProxy(proxyHost);
    }

    // Attach default request config
    clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
  }

  private static byte[] decompressBytes(byte[] compressedArray) throws IOException {
    if (compressedArray == null) {
      return null;
    }

    ByteArrayOutputStream out;
    try (BrotliInputStream is = new BrotliInputStream(new ByteArrayInputStream(compressedArray))) {
      out = new ByteArrayOutputStream();
      if (!BrotliDecoder.decompress(out, is)) {
        return null;
      }
      return out.toByteArray();
    }
  }
}
