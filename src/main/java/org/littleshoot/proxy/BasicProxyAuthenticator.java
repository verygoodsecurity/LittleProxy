package org.littleshoot.proxy;

import com.google.common.io.BaseEncoding;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Basic Auth user/password authenticator
 */
public abstract class BasicProxyAuthenticator extends AbstractProxyAuthenticator {

  private static final Logger LOG = LoggerFactory.getLogger(BasicProxyAuthenticator.class);

  @Override
  public boolean authenticate(HttpRequest request) {
    if (!request.headers().contains(HttpHeaders.Names.PROXY_AUTHORIZATION)) {
      return false;
    }

    List<String> values = request.headers().getAll(HttpHeaders.Names.PROXY_AUTHORIZATION);
    String fullValue = values.iterator().next();
    String value = StringUtils.substringAfter(fullValue, "Basic ").trim();

    if (StringUtils.isNotEmpty(value)) {
      byte[] decodedValue = BaseEncoding.base64().decode(value);
      String decodedString = new String(decodedValue, Charset.forName("UTF-8"));

      String userName = StringUtils.substringBefore(decodedString, ":");
      String password = StringUtils.substringAfter(decodedString, ":");

      return authenticate(userName, password, request);
    }

    LOG.debug("Invalid authentication scheme. Expected 'Basic'");
    return false;
  }

  public abstract boolean authenticate(String username, String password, HttpRequest request);
}
