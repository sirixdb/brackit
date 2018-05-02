/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 * @author Caetano Sauer
 * @author Sebastian Baechle
 *
 */
public class URIHandler {

  public static final int TIMEOUT = 2000;

  public static OutputStream getOutputStream(String uri, boolean overwrite) throws IOException {
    try {
      return getOutputStream(new URI(uri), overwrite);
    } catch (URISyntaxException e) {
      throw new IOException("Invalid resource URI: " + uri, e);
    }
  }

  public static OutputStream getOutputStream(URI uri, boolean overwrite) throws IOException {
    String scheme = uri.getScheme();
    if ((scheme == null) || (scheme.equals("file"))) {
      // handle files locally
      String fullPath = uri.getSchemeSpecificPart();
      if (fullPath == null) {
        throw new IOException(String.format("Illegal file name: %s", uri));
      }
      if (fullPath.startsWith("//")) {
        fullPath = fullPath.substring(1);
      }
      File f = new File(fullPath);
      if (f.exists() && !f.isFile()) {
        throw new IOException(String.format("Location is not a file: %s", uri));
      }
      if (overwrite) {
        if (f.exists())
          f.delete();
        f.createNewFile();
      }
      return new FileOutputStream(f);
    } else if (scheme.equals("http") || scheme.equals("https") || scheme.equals("ftp")
        || scheme.equals("jar")) {
      URL url = uri.toURL();
      return new URLOutputStream(url, TIMEOUT);
    } else {
      throw new IOException(String.format("Unsupported protocol: %s", scheme));
    }
  }

  public static InputStream getInputStream(String uri) throws IOException {
    try {
      return getInputStream(new URI(uri));
    } catch (URISyntaxException e) {
      return Files.newInputStream(Paths.get(uri));
      // throw new IOException("Invalid resource URI: " + uri, e);
    }
  }

  public static InputStream getInputStream(URI uri) throws IOException {
    String scheme = uri.getScheme();
    if ((scheme == null) || (scheme.equals("file"))) {
      // handle files locally
      String fullPath = uri.getSchemeSpecificPart();
      if (fullPath == null) {
        throw new IOException(String.format("Illegal file name: %s", uri));
      }
      if (fullPath.startsWith("//")) {
        fullPath = fullPath.substring(1);
      }
      return new FileInputStream(new File(fullPath));
    } else if (scheme.equals("http") || scheme.equals("https") || scheme.equals("ftp")
        || scheme.equals("jar")) {
      return new URLInputStream(uri.toURL(), TIMEOUT);
    } else {
      throw new IOException(String.format("Unsupported protocol: %s", scheme));
    }
  }

  public static URI getURIForFileName(String path) throws URISyntaxException {
    return new URI("file", null, path, null);
  }

  public static URI getURIForFile(File file) {
    return file.toURI();
  }
}
