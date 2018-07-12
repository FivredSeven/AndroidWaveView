/*
 * @(#)IOUtils.java		       Project:com.sinaapp.msdxblog.androidkit
 * Date:2012-5-6
 *
 * Copyright (c) 2011 CFuture09, Institute of Software, 
 * Guangdong Ocean University, Zhanjiang, GuangDong, China.
 * All rights reserved.
 *
 * 此文件代码完全抽取自apache开源项目commons中的commons-io包。
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.suo.waveform.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 此文件代码完全抽取自apache开源项目commons中的commons-io包。
 *
 * @author Github Geek_Soledad
 */
public class IOUtils {
    /**
     * Unconditionally close a <code>Closeable</code>.
     * <p/>
     * Equivalent to {@link Closeable#close()}, except any exceptions will be
     * ignored. This is typically used in finally blocks.
     * <p/>
     * Example code:
     * <p/>
     * <pre>
     * Closeable closeable = null;
     * try {
     * 	closeable = new FileReader(&quot;foo.txt&quot;);
     * 	// process closeable
     * 	closeable.close();
     * } catch (Exception e) {
     * 	// error handling
     * } finally {
     * 	FileUtils.closeQuietly(closeable);
     * }
     * </pre>
     *
     * @param closeable the object to close, may be null or already closed
     * @since Commons IO 2.0
     */
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    /**
     * The default buffer size to use.
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p/>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since Commons IO 1.3
     */
    public static long copyLarge(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

}
