package org.salgar.igw2trade.runner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.text.StyledEditorKit.BoldAction;

import org.apache.commons.chain.Chain;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.chain.impl.ContextBase;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.salgar.igw2trade.commands.AuthenticateCommand;
import org.salgar.igw2trade.commands.DetailAnalysisCommand;
import org.salgar.igw2trade.commands.SearchCommand;

public class ChainRunner {
	private static final Logger log = Logger.getLogger(ChainRunner.class);

	@SuppressWarnings({ "unchecked", "unused" })
	public void runChain() throws IOException {
		Properties props = new Properties();
		InputStream is = new FileInputStream("password.txt");
		props.load(is);
		
		
		CookieStore cookieStore = new BasicCookieStore();
		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		DefaultHttpClient client = (DefaultHttpClient) wrapClient(new DefaultHttpClient());
		if(Boolean.valueOf(props.getProperty("proxy.enabled"))) {
			HttpHost proxy = new HttpHost("proxy-bn.bn.detemobil.de", 3128);
			client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			client.getParams()
					.setParameter(
							"User-Agent",
							"Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.17) Gecko/20110420 Firefox/3.6.17");
	
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
					props.getProperty("proxy.username"), props.getProperty("proxy.password"));
			client.getCredentialsProvider().setCredentials(
					new AuthScope("proxy-bn.bn.detemobil.de", 3128), credentials);
		}
		Context context = new ContextBase();

		context.put("cookiestore", cookieStore);
		context.put("localContext", localContext);
		context.put("client", client);
		context.put("searchlevel", "50");
		context.put("guildwarstrade_url", "http://www.guildwarstrade.com/item/");
		context.put("minimum_amount_of_demand", 200);
		context.put("profit_margin_against_vendor_value", 3);
		context.put("operation_bracket_top", 1500);
		context.put("operation_bracket_bottom", 50);
		context.put("ratio_of_buy_offer_to_sell_offer", 2.0);
		context.put("minimum_buy_demand", 50);
		context.put("maximum_buy_offer", 1501);
		context.put("gw2.username", props.getProperty("gw2.username"));
		context.put("gw2.pass", props.getProperty("gw2.password"));

		Chain chain = new ChainBase();
		chain.addCommand(new AuthenticateCommand());
		chain.addCommand(new SearchCommand());
		chain.addCommand(new DetailAnalysisCommand());

		try {
			chain.execute(context);
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
