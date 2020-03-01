/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.compression;

import java.util.Random;

/*
This is a helper class that Netty uses in their test sources for building the sample data buffers
to test their compression and decompression codecs. The latest version can be found here:

   https://github.com/netty/netty/blob/4.1/codec/src/test/java/io/netty/handler/codec/compression/AbstractCompressionTest.java

 */
public abstract class AbstractCompressionTest {

    protected static final Random rand;

    protected static final byte[] BYTES_SMALL = new byte[256];
    protected static final byte[] BYTES_LARGE = new byte[256 * 1024];

    static {
        rand = new Random();
        fillArrayWithCompressibleData(BYTES_SMALL);
        fillArrayWithCompressibleData(BYTES_LARGE);
    }

    private static void fillArrayWithCompressibleData(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = i % 4 != 0 ? 0 : (byte) rand.nextInt();
        }
    }
}