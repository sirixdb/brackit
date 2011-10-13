package org.brackit.xquery.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 * 
 * @author Caetano Sauer
 * @author Sebastian Baechle
 * 
 */
public class URIHandler {

	public static final int TIMEOUT = 2000;

	public static OutputStream getOutputStream(URI uri, boolean overwrite)
			throws IOException {
		String scheme = uri.getScheme();
		if ((scheme == null) || (scheme.equals("file"))) {
			// handle files locally
			String fullPath = uri.getSchemeSpecificPart();
			if (fullPath == null) {
				throw new IOException(String.format("Illegal file name: %s",
						uri));
			}
			if (fullPath.startsWith("//")) {
				fullPath = fullPath.substring(1);
			}
			File f = new File(fullPath);
			if (f.exists() && !f.isFile()) {
				throw new IOException(String.format(
						"Location is not a file: %s", uri));
			}
			if (overwrite) {
				if (f.exists())
					f.delete();
				f.createNewFile();
			}
			return new FileOutputStream(f);
		} else if (scheme.equals("http") || scheme.equals("https")
				|| scheme.equals("ftp") || scheme.equals("jar")) {
			URL url = uri.toURL();
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(TIMEOUT);
			return conn.getOutputStream();
		} else {
			throw new IOException(String.format("Unsupported protocol: %s",
					scheme));
		}
	}

	public static InputStream getInputStream(URI uri) throws IOException {
		String scheme = uri.getScheme();
		if ((scheme == null) || (scheme.equals("file"))) {
			// handle files locally
			String fullPath = uri.getSchemeSpecificPart();
			if (fullPath == null) {
				throw new IOException(String.format("Illegal file name: %s",
						uri));
			}
			if (fullPath.startsWith("//")) {
				fullPath = fullPath.substring(1);
			}
			return new FileInputStream(new File(fullPath));
		} else if (scheme.equals("http") || scheme.equals("https")
				|| scheme.equals("ftp") || scheme.equals("jar")) {
			URL url = uri.toURL();
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(TIMEOUT);
			return conn.getInputStream();
		} else {
			throw new IOException(String.format("Unsupported protocol: %s",
					scheme));
		}
	}

	public static URI getURIForFileName(String path) throws URISyntaxException {
		return new URI("file", null, path, null);
	}

	public static URI getURIForFile(File file) {
		return file.toURI();
	}
}
