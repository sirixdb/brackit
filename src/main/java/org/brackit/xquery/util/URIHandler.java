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

import org.brackit.xquery.xdm.DocumentException;

/**
 * 
 * @author Caetano Sauer
 * @author Sebastian Baechle
 * 
 */
public class URIHandler {

	public static OutputStream getOutputStream(URI uri, boolean overwrite)
			throws DocumentException {
		try {
			String scheme = uri.getScheme();
			if ((scheme == null) || (scheme.equals("file"))) {
				// handle files locally
				String fullPath = uri.getSchemeSpecificPart();
				if (fullPath == null) {
					throw new DocumentException("Illegal file name: %s", uri);
				}
				if (fullPath.startsWith("//")) {
					fullPath = fullPath.substring(1);
				}
				File f = new File(fullPath);
				if (f.exists() && !f.isFile()) {
					throw new DocumentException("Location is not a file: %s",
							uri);
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
				return conn.getOutputStream();
			} else {
				throw new DocumentException("Unsupported protocol: %s", scheme);
			}
		} catch (IOException e) {
			throw new DocumentException(e);
		}
	}

	public static InputStream getInputStream(URI uri) throws DocumentException {
		try {
			String scheme = uri.getScheme();
			if ((scheme == null) || (scheme.equals("file"))) {
				// handle files locally
				String fullPath = uri.getSchemeSpecificPart();
				if (fullPath == null) {
					throw new DocumentException("Illegal file name: %s", uri);
				}
				if (fullPath.startsWith("//")) {
					fullPath = fullPath.substring(1);
				}
				return new FileInputStream(new File(fullPath));
			} else if (scheme.equals("http") || scheme.equals("https")
					|| scheme.equals("ftp") || scheme.equals("jar")) {
				URL url = uri.toURL();
				URLConnection conn = url.openConnection();
				return conn.getInputStream();
			} else {
				throw new DocumentException("Unsupported protocol: %s", scheme);
			}
		} catch (IOException e) {
			throw new DocumentException(e);
		}
	}

	public static URI getURIForFileName(String path)
			throws URISyntaxException {
		return new URI("file", null, path, null);
	}

	public static URI getURIForFile(File file) {
		return file.toURI();
	}
}
