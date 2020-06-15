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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;

/**
 * @author Henrique Valer
 */
public class IOUtils {

  public static PrintStream createBuffer() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    return new PrintStream(out) {
      final OutputStream baos = out;

      public String toString() {
        return baos.toString();
      }
    };
  }

  /**
   * Returns MD5 hash sequence for a given String input.
   *
   * @param pInput
   * @return
   */
  public static String getMd5(String pInput) {
    try {
      MessageDigest lDigest;
      lDigest = MessageDigest.getInstance("MD5");
      lDigest.update(pInput.getBytes());
      BigInteger lHashInt = new BigInteger(1, lDigest.digest());
      return String.format("%1$032X", lHashInt);
    } catch (Exception e) {
      return "";
    }

  }

  public static FileFilter nameFilter(final String pattern) {
    return new FileFilter() {
      final String p = pattern;

      @Override
      public boolean accept(File pathname) {
        return pathname.getName().matches(p);
      }
    };
  }

  /**
   * Returns a list of files from a given folder, respecting the given filter.
   * The list of files is extracted recursively.
   *
   * @param dir
   * @param fileFilter
   * @return
   * @throws FileNotFoundException
   */
  public static List<File> getFilteredFileListing(File dir, FileFilter fileFilter) throws FileNotFoundException {
    List<File> result = new ArrayList<File>();
    List<File> filesDirs = Arrays.asList(dir.listFiles());

    for (File file : filesDirs) {
      if (file != null) {
        if (fileFilter.accept(file)) {
          result.add(file);
        }
      }
      if (file.isDirectory()) {
        result.addAll(getFilteredFileListing(file, fileFilter));
      }
    }
    return result;
  }

  /**
   * Returns a list of folders from the given folder. The list is not
   * recursive and removes svn folders from the result.
   *
   * @param dir
   * @return
   * @throws FileNotFoundException
   */
  public static List<File> getFoldersFileListing(File dir) throws FileNotFoundException {
    List<File> result = new ArrayList<File>();
    List<File> filesDirs = Arrays.asList(dir.listFiles());

    for (File file : filesDirs) {
      // remove svn files
      if ((file.isDirectory()) && (!file.getName().startsWith(".svn"))) {
        result.add(file);
      }
    }
    return result;
  }

  /**
   * Returns the 'normalized' path from a file, i.e. the resulting path string
   * contains only slashes as folder separators.
   */
  public static String getNormalizedPath(File f) {
    return f.getPath().replaceAll(Pattern.quote(File.separator), "/");
  }

  /**
   * Return the string content of a file.
   */
  public static String getStringFromFile(File pFile) throws QueryException {
    byte[] buffer = new byte[(int) pFile.length()];
    BufferedInputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(pFile));
      in.read(buffer);
    } catch (IOException e) {
      throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e.getMessage());
    } finally {
      if (in != null)
        try {
          in.close();
        } catch (IOException ignored) {
        }
    }
    return new String(buffer);
  }

  /**
   * Return the string content of an input stream.
   */
  public static String getStringFromInputStream(InputStream in) throws QueryException {

    StringBuffer out = new StringBuffer();
    byte[] b = new byte[4096];
    try {
      for (int n; (n = in.read(b)) != -1; ) {
        out.append(new String(b, 0, n));
      }
    } catch (IOException e) {
      throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e.getMessage());
    } finally {
      if (in != null)
        try {
          in.close();
        } catch (IOException ignored) {
        }
    }

    return out.toString();
  }

}
