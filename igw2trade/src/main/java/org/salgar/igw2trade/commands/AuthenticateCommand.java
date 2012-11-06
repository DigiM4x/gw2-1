package org.salgar.igw2trade.commands;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

public class AuthenticateCommand implements Command {
	private static final Logger log = Logger.getLogger(AuthenticateCommand.class);

	public boolean execute(Context context) throws Exception {
		String username = (String) context.get("gw2.username");
		String pass = (String) context.get("gw2.pass");
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
		
		List<NameValuePair> nv = new ArrayList<NameValuePair>();
		nv.add(new BasicNameValuePair("email", username));
		nv.add(new BasicNameValuePair("password", pass));
		
		post.setEntity(new UrlEncodedFormEntity(nv, "UTF-8"));
		
		log.info("Authenticating");
		
		DefaultHttpClient client = (DefaultHttpClient) context.get("client"); 
		HttpContext localContext = (HttpContext) context.get("localContext");
		
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
		return false;
	}

}
