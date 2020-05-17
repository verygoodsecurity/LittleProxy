package io.netty.handler.codec.compression;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpContentEncoder.Result;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.netty.handler.codec.compression.BrotliHttpContentCompressor.CONTENT_COMPRESSION_ATTRIBUTE;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class BrotliHttpContentCompressorTest {

  private BrotliHttpContentCompressor compressor;
  private Channel channel;

  @Before
  public void setUp() throws Exception {
    compressor = new BrotliHttpContentCompressor();
    ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
    channel = new EmbeddedChannel();
    when(ctx.channel()).thenReturn(channel);
    compressor.handlerAdded(ctx);
  }

  @DataProvider
  public static Object[][] returnsNull() {
    return new Object[][]{
        {"aaa", ",br", "brbr"},  // nonNull contentEncodingHeaderValue
        {"dss", "sss", "br"},    // nonNull contentEncodingHeaderValue
        {"gzip", null, "brbr"},  // nonNull contentEncodingHeaderValue
        {"", "br", null},        // nonNull contentEncodingHeaderValue
        {null, "br", null},     // no attr
        {null, "br", ""},       // no 'br' attr
        {null, "br", "sss"},    // no 'br' attr
        {null, "Br", "Br"},    // no 'br' acceptEncoding
        {null, "bR", "bR"},    // no 'br' acceptEncoding
        {null, "", "br"},      // no 'br' acceptEncoding
    };
  }

  @Test
  @UseDataProvider("returnsNull")
  public void testReturnsNull(String contentEncodingHeaderValue, String acceptEncoding,
      String compressionAttr) throws Exception {
    channel.attr(CONTENT_COMPRESSION_ATTRIBUTE).set(compressionAttr);
    HttpResponse response = mock(HttpResponse.class);
    HttpHeaders headers = new DefaultHttpHeaders();
    if (contentEncodingHeaderValue != null) {
      headers.set(HttpHeaderNames.CONTENT_ENCODING, contentEncodingHeaderValue);
    }
    when(response.headers()).thenReturn(headers);
    Result result = compressor.beginEncode(response, acceptEncoding);
    assertNull(result);
  }

  @DataProvider
  public static Object[][] returnsCoder() {
    return new Object[][]{
        {null, "br", "br", notNullValue()},
        {null, "br", "br,gzip", notNullValue()},
        {null, "br", "gzip, br", notNullValue()},
        {null, "br", "gzip, brotli", notNullValue()},
        {null, "br", "gzip, brotlI", notNullValue()},
    };
  }

  @Test
  @UseDataProvider("returnsCoder")
  public void testReturnsCoder(String contentEncodingHeaderValue, String acceptEncoding,
      String compressionAttr, Matcher brotli) throws Exception {
    channel.attr(CONTENT_COMPRESSION_ATTRIBUTE).set(compressionAttr);
    HttpResponse response = mock(HttpResponse.class);
    HttpHeaders headers = new DefaultHttpHeaders();
    if (contentEncodingHeaderValue != null) {
      headers.set(HttpHeaderNames.CONTENT_ENCODING, contentEncodingHeaderValue);
    }
    when(response.headers()).thenReturn(headers);
    Result result = compressor.beginEncode(response, acceptEncoding);
    assertNotNull(result);
    assertThat(result.contentEncoder().pipeline().get(BrotliEncoder.class), brotli);
  }
}
