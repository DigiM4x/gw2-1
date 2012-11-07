package org.salgar.igw2trade.commands;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

public class DetailAnalysisCommand implements Command {
	private static final Logger log = Logger
			.getLogger(DetailAnalysisCommand.class);

	public boolean execute(Context context) throws Exception {
		DefaultHttpClient client = (DefaultHttpClient) context.get("client");
		CookieStore cookieStore = (CookieStore) context.get("cookiestore");
		String searchLevel = (String) context.get("searchlevel");
		String url = (String) context.get("guildwarstrade_url");
		int ratioOfBuyOfferToSellOfferValue = (Integer) context
				.get("ratio_of_buy_offer_to_sell_offer");
		int minimumBuyDemand = (Integer) context.get("minimum_buy_demand");
		int maximumBuyOffer = (Integer) context.get("maximum_buy_offer");

		@SuppressWarnings("unchecked")
		List<String> worthAnalyzing = (List<String>) context
				.get("worthAnalyzing");

		String pattern = "\"data_id\":\"";
		List<String> dataIds = new ArrayList<String>();
		for (int i = 0, n = worthAnalyzing.size(); i < n; i++) {
			String object = worthAnalyzing.get(i);
			String data_id = object.substring(
					object.indexOf(pattern) + pattern.length(),
					object.indexOf("\"",
							object.indexOf(pattern) + pattern.length()));
			dataIds.add(data_id);
		}

		String ids = StringUtils.join(dataIds, ",");
		if (!"".equals(ids)) {
			analyze(client, cookieStore, ids, searchLevel, url,
					ratioOfBuyOfferToSellOfferValue, minimumBuyDemand, maximumBuyOffer);
		}
		return false;
	}

	@SuppressWarnings("unused")
	private void analyze(DefaultHttpClient client, CookieStore cookieStore,
			String ids, String searchLevel, String url,
			int ratioOfBuyOfferToSellOfferValue, int minimumBuyDemand, int maximumBuyOffer)
			throws ClientProtocolException, IOException {
		HttpGet get = new HttpGet(
				"https://tradingpost-live.ncplatform.net/ws/search.json?ids="
						+ ids + "&levelmin=" + searchLevel + "&levelmax="
						+ searchLevel);

		get.setHeader("X-Requested-With", "XMLHttpRequest");
		get.addHeader("Cookie", "s="
				+ cookieStore.getCookies().get(0).getValue());
		HttpResponse responseGet = client.execute(get);
		List<Cookie> cookies = client.getCookieStore().getCookies();

		HttpEntity entity = responseGet.getEntity();

		BufferedInputStream bis = new BufferedInputStream(entity.getContent());
		int length = 0;
		byte[] buff = new byte[1024];
		StringBuffer sb = new StringBuffer(1024);
		while ((length = bis.read(buff)) != -1) {
			sb.append(new String(buff, 0, length, "UTF-8"));
		}

		String result = sb.toString();
		log.info("Analyzed!");
		log.info(result);

		int i = 0;

		List<String> flipCandidate = new ArrayList<String>();
		while ((i = result.indexOf("{\"type_id", i)) > -1) {
			int end = result.indexOf("}", i + 1);
			String object = result.substring(i, end + 1);
			String[] fields = object.split(",");

			int buyOffer = 0;
			int saleOffer = 0;
			int demandValue = 0;
			int vendorValue = 0;

			for (int j = 0; j < fields.length; j++) {
				if (fields[j].indexOf("max_offer_unit_price") > -1) {
					String[] bOffer = fields[j].split(":");
					buyOffer = Integer.valueOf(bOffer[1].substring(1,
							bOffer[1].length() - 1));
				} else if (fields[j].indexOf("min_sale_unit_price") > -1) {
					String[] sOffer = fields[j].split(":");
					saleOffer = Integer.valueOf(sOffer[1].substring(1,
							sOffer[1].length() - 1));
				} else if (fields[j].indexOf("offer_availability") > -1) {
					String[] demand = fields[j].split(":");

					demandValue = Integer.valueOf(demand[1].substring(1,
							demand[1].length() - 1));
				} else if (fields[j].indexOf("vendor_sell_price") > -1) {
					String[] vendor = fields[j].split(":");

					vendorValue = Integer.valueOf(vendor[1].substring(1,
							vendor[1].length() - 1));
				}
			}
			if (saleOffer >= ratioOfBuyOfferToSellOfferValue * buyOffer
					&& demandValue >= minimumBuyDemand) {
				if (buyOffer >= vendorValue && buyOffer <= maximumBuyOffer) {
					flipCandidate.add(object);
				} else if ((saleOffer >= ratioOfBuyOfferToSellOfferValue
						* (vendorValue == 0 ? 1 : vendorValue)) && (vendorValue <= maximumBuyOffer) ) {
					flipCandidate.add(object);
				}
			}
			i = end;
		}

		log.info("Flip candidates: ");

		String pattern = "\"data_id\":\"";
		for (String object : flipCandidate) {
			String data_id = object.substring(
					object.indexOf(pattern) + pattern.length(),
					object.indexOf("\"",
							object.indexOf(pattern) + pattern.length()));
			System.out.println("<a href=\"" + url + data_id + "\">" + url + data_id + "</a></br>");
			// log.info(url + data_id);

		}
	}

}
