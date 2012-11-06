package org.salgar.igw2trade;

import java.io.BufferedInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.params.CookieSpecPNames;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Authentication {
	private static final Logger log = Logger.getLogger(Authentication.class);

	public void initialize() throws Throwable {
		CookieStore cookieStore = new BasicCookieStore();
		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		//localContext.setAttribute(ClientContext.COOKIE_SPEC, new CustomSpec()); 
		
		
		HttpPost post = new HttpPost("https://account.guildwars2.com/login");
		post.setHeader(
				"User-Agent",
				"Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.17) Gecko/20110420 Firefox/3.6.17");
		post.setHeader("Host", "account.guildwars2.com");
		post.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		post.setHeader("Accept-Language", "en-US,en;q=0.5");
		post.setHeader("Connection", "keep-alive");
		post.setHeader("DNT", "1");
		post.setHeader("Referer", "https://account.guildwars2.com/login");
		post.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.NETSCAPE);
		List<String> datePatterns = new ArrayList<String>();
		datePatterns.add("EEE, dd MMM yyyy HH:mm:ss z");
		post.getParams().setParameter(CookieSpecPNames.DATE_PATTERNS, datePatterns);
		

		DefaultHttpClient client = (DefaultHttpClient) wrapClient(new DefaultHttpClient());

		HttpGet get = new HttpGet("https://tradingpost-live.ncplatform.net/ws/search.json?text=" 
				/*+ URLEncoder.encode("*", "UTF-8")*/
				+ "&offset=11&levelmin=60&levelmax=60&count=100");
		
//		HttpGet get = new HttpGet("https://tradingpost-live.ncplatform.net/ws/search.json?ids=13716&levelmin=56&levelmax=64");
		
//		HttpGet get = new HttpGet("https://tradingpost-live.ncplatform.net/ws/listings.json?id=13716&type=all");
		
		get.setHeader(
				"X-Requested-With",
				"XMLHttpRequest");
		
		HttpHost proxy = new HttpHost("proxy-bn.bn.detemobil.de", 3128);
		client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		client.getParams().setParameter("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.17) Gecko/20110420 Firefox/3.6.17");

		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
				"*****", "*****");
		client.getCredentialsProvider().setCredentials(
				new AuthScope("proxy-bn.bn.detemobil.de", 3128), credentials);

		List<NameValuePair> nv = new ArrayList<NameValuePair>();
		nv.add(new BasicNameValuePair("email", "*******"));
		nv.add(new BasicNameValuePair("password", "*********"));

		post.setEntity(new UrlEncodedFormEntity(nv, "UTF-8"));

		try {
			log.info("Authenticating");
			HttpResponse responsePost = client.execute(post, localContext);									
			List<Cookie> cookies = client.getCookieStore().getCookies();

			HttpEntity entityPost = responsePost.getEntity();
			
			StatusLine status = responsePost.getStatusLine();
			
			BufferedInputStream bisPost = new BufferedInputStream(
					entityPost.getContent());
			int lengthPost = 0;
			byte[] buffPost = new byte[1024];
			StringBuffer sbPost = new StringBuffer(1024);
			while ((lengthPost = bisPost.read(buffPost)) != -1) {
				sbPost.append(new String(buffPost, 0, lengthPost, "UTF-8"));
			}

			String resultPost = sbPost.toString();
			log.info(resultPost);
			
			log.info("Authenticated");
			
			get.addHeader("Cookie", "s=" + cookieStore.getCookies().get(0).getValue());
			HttpResponse responseGet = client.execute(get);
			cookies = client.getCookieStore().getCookies();
			
			HttpEntity entity = responseGet.getEntity();
			
			BufferedInputStream bis = new BufferedInputStream(
					entity.getContent());
			int length = 0;
			byte[] buff = new byte[1024];
			StringBuffer sb = new StringBuffer(1024);
			while ((length = bis.read(buff)) != -1) {
				sb.append(new String(buff, 0, length, "UTF-8"));
			}

			String result = sb.toString();
			log.info("Got the list!");
			log.info(result);
			int i = 0;
			
			while ( (i = result.indexOf("{\"type_id", i)) > -1) {
				int end = result.indexOf("}", i + 1);
				String object = result.substring(i,  end + 1);
				String[] fields = object.split(",");
				for (int j = 0, n = fields.length; j < n; j++) {
					if (fields[j].indexOf("count") > -1) {
						String[] count = fields[j].split(":");
						int countValue = Integer.valueOf(count[1].substring(1, count[1].length() - 1));
						
						if (countValue >= 100) {
							log.info(object);
						}
						break;					
					}					
				}
				i = end;
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}

	public static HttpClient wrapClient(HttpClient base) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {

				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, 443));
			return new DefaultHttpClient(ccm, base.getParams());
		} catch (Exception ex) {
			return null;
		}
	}
}
