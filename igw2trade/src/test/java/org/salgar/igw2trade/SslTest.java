package org.salgar.igw2trade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.testng.annotations.Test;

@Test
public class SslTest {
	public static final String TARGET_HTTPS_SERVER = "www.verisign.com";
	public static final int TARGET_HTTPS_PORT = 443;

	@Test
	public void test() throws Exception {
		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory
		.getDefault();
		
		final char[] pass = "********".toCharArray();
		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				PasswordAuthentication pa = new PasswordAuthentication(
						"******", pass);
				return pa;
			}

		});

		@SuppressWarnings("unused")
		SocketAddress addr = new InetSocketAddress("proxy-bn.bn.detemobil.de",
				3128);
		//Proxy proxy = new Proxy(Proxy.Type.SOCKS, addr);
		Socket socket = new Socket("proxy-bn.bn.detemobil.de", 3128);
		doTunnelHandshake(socket, TARGET_HTTPS_SERVER, TARGET_HTTPS_PORT);
		
		SSLSocket socketSsl = (SSLSocket) factory.createSocket(socket,
				TARGET_HTTPS_SERVER, TARGET_HTTPS_PORT, false);
		socketSsl.startHandshake();
		try {
			Writer out = new OutputStreamWriter(socketSsl.getOutputStream(),
					"ISO-8859-1");
			out.write("GET / HTTP/1.1\r\n");
			out.write("Host: " + TARGET_HTTPS_SERVER + ":" + TARGET_HTTPS_PORT
					+ "\r\n");
			out.write("Agent: SSL-TEST\r\n");
			out.write("\r\n");
			out.flush();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					socketSsl.getInputStream(), "ISO-8859-1"));
			String line = null;
			while ((line = in.readLine()) != null) {
				System.out.println(line);
			}
		} finally {
			socketSsl.close();
		}

	}

	private void doTunnelHandshake(Socket tunnel, String host, int port)
			throws IOException {
		OutputStream out = tunnel.getOutputStream();
		String msg = "CONNECT " + host + ":" + port + " HTTP/1.0\n"
				+ "User-Agent: "
				+ sun.net.www.protocol.http.HttpURLConnection.userAgent
				+ "\r\n\r\n";
		byte b[];
		try {
			/*
			 * We really do want ASCII7 -- the http protocol doesn't change with
			 * locale.
			 */
			b = msg.getBytes("ASCII7");
		} catch (UnsupportedEncodingException ignored) {
			/*
			 * If ASCII7 isn't there, something serious is wrong, but Paranoia
			 * Is Good (tm)
			 */
			b = msg.getBytes();
		}
		out.write(b);
		out.flush();

		/*
		 * We need to store the reply so we can create a detailed error message
		 * to the user.
		 */
		byte reply[] = new byte[200];
		int replyLen = 0;
		int newlinesSeen = 0;
		boolean headerDone = false; /* Done on first newline */

		InputStream in = tunnel.getInputStream();
		boolean error = false;

		while (newlinesSeen < 2) {
			int i = in.read();
			if (i < 0) {
				throw new IOException("Unexpected EOF from proxy");
			}
			if (i == '\n') {
				headerDone = true;
				++newlinesSeen;
			} else if (i != '\r') {
				newlinesSeen = 0;
				if (!headerDone && replyLen < reply.length) {
					reply[replyLen++] = (byte) i;
				}
			}
		}

		/*
		 * Converting the byte array to a string is slightly wasteful in the
		 * case where the connection was successful, but it's insignificant
		 * compared to the network overhead.
		 */
		String replyStr;
		try {
			replyStr = new String(reply, 0, replyLen, "ASCII7");
		} catch (UnsupportedEncodingException ignored) {
			replyStr = new String(reply, 0, replyLen);
		}

		/*
		 * We check for Connection Established because our proxy returns
		 * HTTP/1.1 instead of 1.0
		 */
		// if (!replyStr.startsWith("HTTP/1.0 200")) {
		if (replyStr.toLowerCase().indexOf("200 connection established") == -1) {
			throw new IOException("Unable to tunnel through "
					+ ".  Proxy returns \"" + replyStr + "\"");
		}

		/* tunneling Handshake was successful! */
	}
}
