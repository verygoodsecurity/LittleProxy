package org.littleshoot.proxy;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BadGatewayFailureHttpResponseComposerTest {

  private static final String REQUEST_URI = "https://localhost/hi";

  @Test
  public void testDefault() throws IOException {
    FailureHttpResponseComposer badGatewayResponseComposer = new BadGatewayFailureHttpResponseComposer();

    HttpRequest initialRequest = mock(HttpRequest.class);
    when(initialRequest.getUri()).thenReturn(REQUEST_URI);

    FullHttpResponse response = badGatewayResponseComposer.compose(initialRequest, new RuntimeException());

    assertEquals(502, response.getStatus().code());
    assertEquals("Bad Gateway", response.getStatus().reasonPhrase());
    assertEquals("Bad Gateway: " + REQUEST_URI, new String(response.content().array()));
  }

  @Test
  public void testCustomMessage() throws IOException {
    FailureHttpResponseComposer badGatewayResponseComposer = new BadGatewayFailureHttpResponseComposer() {
      @Override
      protected String provideCustomMessage(HttpRequest httpRequest, Throwable cause) {
        return "Invalid certificate: " + httpRequest.getUri();
      }
    };

    HttpRequest initialRequest = mock(HttpRequest.class);
    when(initialRequest.getUri()).thenReturn(REQUEST_URI);

    FullHttpResponse response = badGatewayResponseComposer.compose(initialRequest, new RuntimeException());

    assertEquals(502, response.getStatus().code());
    assertEquals("Bad Gateway", response.getStatus().reasonPhrase());
    assertEquals("Invalid certificate: " + REQUEST_URI, new String(response.content().array()));
  }

  @Test
  public void testClearedContent() throws IOException {
    FailureHttpResponseComposer badGatewayResponseComposer = new BadGatewayFailureHttpResponseComposer();

    HttpRequest initialRequest = mock(HttpRequest.class);
    when(initialRequest.getUri()).thenReturn(REQUEST_URI);

    FullHttpResponse response = badGatewayResponseComposer.compose(initialRequest, new RuntimeException());

    assertEquals(502, response.getStatus().code());

    assertEquals(0, response.content().readerIndex());
    assertNotEquals(0, response.content().writerIndex());

    when(initialRequest.getMethod()).thenReturn(HttpMethod.HEAD);

    response = badGatewayResponseComposer.compose(initialRequest, new RuntimeException());

    assertEquals(502, response.getStatus().code());

    assertEquals(0, response.content().readerIndex());
    assertEquals(0, response.content().writerIndex());
  }

}