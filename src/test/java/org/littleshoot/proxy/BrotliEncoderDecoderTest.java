package org.littleshoot.proxy;

import com.google.common.io.Resources;

import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import com.nixxcode.jvmbrotli.enc.Encoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.entity.DecompressingEntity;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.GzipHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.junit.Before;
import org.junit.Test;
import org.littleshoot.proxy.extras.SelfSignedSslEngineSource;

import sun.net.www.content.text.PlainTextInputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import io.netty.handler.codec.compression.BrotliDecoder;

import static java.lang.Float.parseFloat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BrotliEncoderDecoderTest extends AbstractProxyTest {

  class BrotliDecompressingEntity extends DecompressingEntity {
    BrotliDecompressingEntity(HttpEntity entity) {
      super(entity, instream -> new BrotliInputStream(instream));
    }
  }

  class HttpAcceptEncodingParser {

    private static final String HTTP_HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String CODING_SEPARATOR = ",";
    private static final String CODING_QVALUE_SEPARATOR = ";";
    private static final String QVALUE_PREFIX = "q=";

    boolean acceptBrotliEncoding(HttpServletRequest httpRequest) {
      return acceptBrotliEncoding(httpRequest.getHeader(HTTP_HEADER_ACCEPT_ENCODING));
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

        if (!BrotliServletFilter.BROTLI_HTTP_CONTENT_CODING.equals(coding_and_qvalue[0].trim())) {
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

  public class BrotliServletOutputStream extends ServletOutputStream {

    private final BrotliOutputStream brotliOutputStream;

    public BrotliServletOutputStream(OutputStream outputStream) throws IOException {
      this(outputStream, new Encoder.Parameters());
    }

    public BrotliServletOutputStream(OutputStream outputStream, Encoder.Parameters parameter) throws IOException {
      brotliOutputStream = new BrotliOutputStream(outputStream, parameter);
    }

    @Override
    public void write(int i) throws IOException {
      brotliOutputStream.write(i);
    }

    @Override
    public void write(byte[] buffer) throws IOException {
      brotliOutputStream.write(buffer);
    }

    @Override
    public void write(byte[] buffer, int offset, int len) throws IOException {
      brotliOutputStream.write(buffer, offset, len);
    }

    @Override
    public void flush() throws IOException {
      brotliOutputStream.flush();
    }

    @Override
    public void close() throws IOException {
      brotliOutputStream.close();
    }

  }

  public class BrotliServletResponseWrapper extends HttpServletResponseWrapper {

    private BrotliServletOutputStream brotliServletOutputStream = null;
    private PrintWriter printWriter = null;
    private final Encoder.Parameters brotliParameter;

    public BrotliServletResponseWrapper(HttpServletResponse response, Encoder.Parameters parameters) {
      super(response);
      brotliParameter = parameters;
    }

    void close() throws IOException {
      if (this.printWriter != null) {
        this.printWriter.close();
      }
      if (this.brotliServletOutputStream != null) {
        this.brotliServletOutputStream.close();
      }
    }

    @Override
    public void flushBuffer() throws IOException {
      if (this.printWriter != null) {
        this.printWriter.flush();
      }
      try {
        if (this.brotliServletOutputStream != null) {
          this.brotliServletOutputStream.flush();
        }
      } finally {
        super.flushBuffer();
      }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      if (this.printWriter != null) {
        throw new IllegalStateException("PrintWriter obtained already - cannot get OutputStream");
      }
      if (this.brotliServletOutputStream == null) {
        this.brotliServletOutputStream = new BrotliServletOutputStream(getResponse().getOutputStream(), brotliParameter);
      }
      return this.brotliServletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      if (this.printWriter == null && this.brotliServletOutputStream != null) {
        throw new IllegalStateException("OutputStream obtained already - cannot get PrintWriter");
      }
      if (this.printWriter == null) {
        this.brotliServletOutputStream = new BrotliServletOutputStream(getResponse().getOutputStream(), brotliParameter);
        this.printWriter = new PrintWriter(new OutputStreamWriter(this.brotliServletOutputStream, getResponse().getCharacterEncoding()));
      }
      return this.printWriter;
    }

    @Override
    public void setContentLength(int len) {
      setContentLengthLong((long) len);
    }

    public void setContentLengthLong(long len) {
      //ignore, since content length of compressed content does not match length of raw content.
    }
  }

  public class BrotliServletFilter implements Filter {

    /**
     * As defined in RFC draft "Brotli Compressed Data Format"
     *
     * @see <a href="http://www.ietf.org/id/draft-alakuijala-brotli-08.txt"></a>
     */
    public static final String BROTLI_HTTP_CONTENT_CODING = "br";
    protected Encoder.Parameters params = new Encoder.Parameters().setQuality(1);
    private final HttpAcceptEncodingParser acceptEncodingParser = new HttpAcceptEncodingParser();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      if (acceptEncodingParser.acceptBrotliEncoding((HttpServletRequest) request)) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.addHeader("Content-Encoding", BROTLI_HTTP_CONTENT_CODING);
        BrotliServletResponseWrapper brResp = new BrotliServletResponseWrapper(httpResponse, params);
        try {
          chain.doFilter(request, brResp);
        } finally {
          brResp.close();
        }
      } else {
        chain.doFilter(request, response);
      }
    }

    @Override
    public void destroy() {

    }
  }

  public class TestResourceHandler extends ResourceHandler {
    public TestResourceHandler() {
      super();
      this.setEtags(true);
      this.setCacheControl("max-age=604800");
      this.setResourceBase(BrotliEncoderDecoderTest.class.getResource("/brotli").getPath());
    }
  }

  private Server webServer;

  protected Server startWebServer(boolean enableHttps) {
    Server s = new Server(0);

    ContextHandlerCollection handlers = new ContextHandlerCollection();
//    addHandler("/csv", new CsvHandler);
    addHandler(handlers, "/gzip", new GzipHandler());
//    addHandler("/deflate", new DeflateCsvHandler);
    addHandler(handlers, "/static", new TestResourceHandler());
    s.setHandler(handlers);
    if (enableHttps) {
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
    }

    try {
      s.start();
    } catch (Exception e) {
      throw new RuntimeException("Error starting Jetty web server", e);
    }
    return s;
  }

  private void addHandler(ContextHandlerCollection handlers, String path, Handler handler) {
    ContextHandler contextHandler = new ContextHandler();
    contextHandler.setContextPath(path);
    contextHandler.setHandler(handler);
    handlers.addHandler(contextHandler);
  }

  @Before
  @Override
  public void runSetUp() throws Exception {
    System.out.println("Got here!");
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

  @Test
  public void brotli_content_encoding_is_set() throws Exception {
    // given
    URL textFileUrl = new URL(webHost + "/static/a100.txt");

    // when
    HttpURLConnection httpCon = (HttpURLConnection) textFileUrl.openConnection();
    httpCon.addRequestProperty(
        //HttpAcceptEncodingParser.HTTP_HEADER_ACCEPT_ENCODING,
        "accept-encoding",
        "gzip");
        //BrotliServletFilter.BROTLI_HTTP_CONTENT_CODING);
    httpCon.connect();

    for(Map.Entry<String, List<String>> entry : httpCon.getHeaderFields().entrySet()) {
      String key = entry.getKey();
      for (String value : entry.getValue()) {
        System.out.println("key: " + key + " ; " + "value: " + value);
      }
    }
    PlainTextInputStream in = (PlainTextInputStream)httpCon.getContent();
    //Iterate over the InputStream and print it out.
    int c;
    while ((c = in.read()) != -1) {
      System.out.print((char) c);
    }

    String contentEncoding = httpCon.getHeaderField("Content-Encoding");
    httpCon.disconnect();

    // then
    assertEquals(contentEncoding, BrotliServletFilter.BROTLI_HTTP_CONTENT_CODING);
  }

  //@Test
  public void content_length_is_NOT_set__OR_is_zero() throws Exception {
    // given
    URL textFileUrl = new URL(webHost + "/canterbury-corpus/alice29.txt");

    // when
    HttpURLConnection httpCon = (HttpURLConnection) textFileUrl.openConnection();
    httpCon.addRequestProperty(
        HttpAcceptEncodingParser.HTTP_HEADER_ACCEPT_ENCODING,
        BrotliServletFilter.BROTLI_HTTP_CONTENT_CODING);
    httpCon.connect();
    String contentEncoding = httpCon.getHeaderField("Content-Length");

    // then
    if (contentEncoding != null) {
      assertEquals(Integer.parseInt(contentEncoding), 0);
    }
    // contentEncoding==null is OK
  }

}