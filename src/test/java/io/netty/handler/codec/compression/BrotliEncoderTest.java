package io.netty.handler.codec.compression;

import com.nixxcode.jvmbrotli.common.BrotliLoader;

import org.junit.Before;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

public class BrotliEncoderTest extends AbstractEncoderTest {

  @Before
  public void setUp() throws Exception {
    BrotliLoader.isBrotliAvailable();
  }

  @Override
  public void initChannel() {
    channel = new EmbeddedChannel(new BrotliEncoder(1));
  }

  @Override
  protected ByteBuf decompress(ByteBuf compressed, int originalLength) throws Exception {
    byte[] compressedArray = new byte[compressed.readableBytes()];
    compressed.readBytes(compressedArray);
    compressed.release();

    byte[] decompressed = BrotliDecoder.decompress(compressedArray);
    return Unpooled.wrappedBuffer(decompressed);
  }
}

