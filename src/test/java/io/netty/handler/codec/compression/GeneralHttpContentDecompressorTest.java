package io.netty.handler.codec.compression;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.embedded.EmbeddedChannel;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class GeneralHttpContentDecompressorTest {

  private GeneralHttpContentDecompressor decompressor;

  @Before
  public void setUp() throws Exception {
    decompressor = new GeneralHttpContentDecompressor();
    ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
    Channel channel = mock(Channel.class);
    when(channel.metadata()).thenReturn(new ChannelMetadata(false));
    ChannelConfig channelConfig = mock(ChannelConfig.class);
    when(channel.config()).thenReturn(channelConfig);
    when(ctx.channel()).thenReturn(channel);
    decompressor.handlerAdded(ctx);
  }


  @DataProvider
  public static Object[][] validCases() {
    return new Object[][]{
        {"br", notNullValue(), nullValue()},
        {"bR", notNullValue(), nullValue()},
        {"BR", notNullValue(), nullValue()},
        {"gzip", nullValue(), notNullValue()},
        {"x-gzip", nullValue(), notNullValue()},
        {"x-deflate", nullValue(), notNullValue()},
        {"deflate", nullValue(), notNullValue()},
    };
  }

  @Test
  @UseDataProvider("validCases")
  public void testNewContentDecoder(String encoding, Matcher brotli, Matcher gzip) throws Exception {
    EmbeddedChannel embeddedChannel = decompressor.newContentDecoder(encoding);
    assertThat(embeddedChannel.pipeline().get(BrotliDecoder.class), brotli);
    assertThat(embeddedChannel.pipeline().get(JdkZlibDecoder.class), gzip);
  }

  @DataProvider
  public static Object[][] invalidCases() {
    return new Object[][]{
        {"ddd", nullValue()},
        {"br, gzip", nullValue()},
        {"deflate,gzip", nullValue()},
    };
  }

  @Test
  @UseDataProvider("invalidCases")
  public void testNewContentDecoderInvalid(String encoding, Matcher nullValue) throws Exception {
    EmbeddedChannel embeddedChannel = decompressor.newContentDecoder(encoding);
    assertThat(embeddedChannel, nullValue);
  }


}
