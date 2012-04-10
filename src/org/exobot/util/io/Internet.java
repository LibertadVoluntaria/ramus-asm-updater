package org.exobot.util.io;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Ramus
 */
public class Internet {

	private static final String userAgent;

	static {
		userAgent = getUserAgent();
	}

	public static byte[] downloadBinary(final String url) throws IOException {
		final URLConnection con = getConnection(url);
		final InputStream in = con.getInputStream();
		final int len = con.getContentLength();
		final byte[] data = new byte[len];
		int off = 0;
		while (off < len) {
			final int read = in.read(data, off, data.length - off);
			if (read == -1) {
				break;
			}
			off += read;
		}
		in.close();
		if (off < len) {
			throw new IOException(String.format("Read %d bytes; expected %d", off, len));
		}
		return data;
	}

	public static String downloadContent(final String url) throws IOException {
		final URLConnection con = getConnection(url);
		final BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String content = "";
		String output;
		while ((output = in.readLine()) != null) {
			content += output + '\n';
		}
		content = content.substring(0, content.length() - 1);
		in.close();
		return content;
	}

	public static void downloadTo(final String url, final File file) throws IOException {
		final byte[] buffer = downloadBinary(url);
		if (!file.exists()) {
			if (!file.createNewFile()) {
				return;
			}
		}
		final FileOutputStream fos = new FileOutputStream(file);
		fos.write(buffer);
		fos.flush();
		fos.close();
	}

	public static HttpURLConnection getConnection(final String url) throws IOException {
		final URL u = new URL(url);
		final HttpURLConnection con = (HttpURLConnection) u.openConnection();
		con.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		con.addRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		con.addRequestProperty("Accept-Encoding", "gzip");
		con.addRequestProperty("Accept-Language", "en-us,en;q=0.5");
		con.addRequestProperty("Host", u.getHost());
		con.addRequestProperty("User-Agent", userAgent);
		con.setConnectTimeout(10000);
		con.setUseCaches(true);
		return con;
	}

	private static String getUserAgent() {
		final boolean x64 = System.getProperty("sun.arch.data.model").equals("64");
		final String osName = System.getProperty("os.name");
		final String os;
		if (osName.contains("Mac")) {
			os = "Macintosh; Intel Mac OS X 10_6_6";
		} else if (osName.contains("Linux")) {
			os = "X11; Linux " + (x64 ? "x86_64" : "i686");
		} else if (osName.contains("Windows")) {
			os = "Windows NT 6.1" + (x64 ? "; WOW64" : "");
		} else {
			os = "";
		}
		final StringBuilder buf = new StringBuilder(128);
		buf.append("Mozilla/5.0 (").append(os).append(")");
		buf.append(" AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.100 Safari/534.30");
		return buf.toString();
	}
}