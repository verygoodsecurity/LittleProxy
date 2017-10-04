package org.littleshoot.proxy;

import io.netty.channel.Channel;

/**
 * Netty is not designed for thread local or global state usage.
 * It guarantees that a channel is handled by only one thread
 * but that thread is constantly reused by other channels so
 * the state can be messed up.
 *
 * This handler lets serialize the state to a channel so it
 * can be deserialized when needed.
 */
public interface GlobalStateHandler {

  /**
   * Serializes global state to channel.
   *
   * @param channel client connection channel
   */
  void persistToChannel(Channel channel);

  /**
   * Deserializes global state from channel.
   *
   * @param channel client connection channel
   */
  void restoreFromChannel(Channel channel);

  /**
   * Clears global state.
   */
  void clear();

}
