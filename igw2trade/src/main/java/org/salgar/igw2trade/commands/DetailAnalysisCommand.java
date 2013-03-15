package org.salgar.igw2trade.commands;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.salgar.igw2trade.domain.AnalyzedObject;
import org.salgar.igw2trade.parser.RecognitionPatterns;

public class DetailAnalysisCommand implements Command {
	private static final Logger log = Logger
			.getLogger(DetailAnalysisCommand.class);

	public boolean execute(Context context) throws Exception {
		DefaultHttpClient client = (DefaultHttpClient) context.get("client");
		CookieStore cookieStore = (CookieStore) context.get("cookiestore");
		String searchLevel = (String) context.get("searchlevel");
		String url = (String) context.get("guildwarstrade_url");
		Double ratioOfBuyOfferToSellOfferValue = Double.valueOf(context
				.get("ratio_of_buy_offer_to_sell_offer").toString());
		int minimumBuyDemand = (Integer) context.get("minimum_buy_demand");
		int maximumBuyOffer = (Integer) context.get("operation_bracket_top");

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
			Double ratioOfBuyOfferToSellOfferValue, int minimumBuyDemand, int maximumBuyOffer)
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

		List<AnalyzedObject> flipCandidate = new ArrayList<AnalyzedObject>();
		while ((i = result.indexOf("{\"type_id", i)) > -1) {
			AnalyzedObject analyzedObject = new AnalyzedObject();
			int end = result.indexOf("}", i + 1);
			String object = result.substring(i, end + 1);
			String[] fields = object.split(",");

			int buyOffer = 0;
			int saleOffer = 0;
			int demandValue = 0;
			int vendorValue = 0;

			for (int j = 0; j < fields.length; j++) {
				if(fields[j].indexOf(RecognitionPatterns.DATA_ID) > -1) {
					String[] data_id = fields[j].split(":");
					analyzedObject.setDataId(Integer.valueOf(data_id[1].substring(1,
							data_id[1].length() - 1)));
				} else if(fields[j].indexOf(RecognitionPatterns.NAME) > -1) {
					String name[] = fields[j].split(":");
					analyzedObject.setName(name[1].substring(1,	name[1].length() - 1));
				} else if (fields[j].indexOf(RecognitionPatterns.BUYING_PRICE) > -1) {
					String[] bOffer = fields[j].split(":");
					buyOffer = Integer.valueOf(bOffer[1].substring(1,
							bOffer[1].length() - 1));
					analyzedObject.setBuyOffer(buyOffer);
				} else if (fields[j].indexOf(RecognitionPatterns.SELLING_PRICE) > -1) {
					String[] sOffer = fields[j].split(":");
					saleOffer = Integer.valueOf(sOffer[1].substring(1,
							sOffer[1].length() - 1));
					analyzedObject.setSalesOffer(saleOffer);
				} else if (fields[j].indexOf(RecognitionPatterns.BUY_ORDERS) > -1) {
					String[] demand = fields[j].split(":");

					demandValue = Integer.valueOf(demand[1].substring(1,
							demand[1].length() - 1));
					analyzedObject.setDemand(demandValue);
				} else if (fields[j].indexOf(RecognitionPatterns.VENDOR_PRICE) > -1) {
					String[] vendor = fields[j].split(":");

					vendorValue = Integer.valueOf(vendor[1].substring(1,
							vendor[1].length() - 1));
					analyzedObject.setVendorValue(vendorValue);
				}
			}
			if (saleOffer >= ratioOfBuyOfferToSellOfferValue * buyOffer
					&& demandValue >= minimumBuyDemand) {
				if (buyOffer >= vendorValue && buyOffer <= maximumBuyOffer) {					
					analyzedObject.setProfitability(Double.valueOf(saleOffer) / Double.valueOf(buyOffer));
					flipCandidate.add(analyzedObject);
				} else if ((saleOffer >= ratioOfBuyOfferToSellOfferValue
						* (vendorValue == 0 ? 1 : vendorValue)) && (vendorValue <= maximumBuyOffer) ) {
					analyzedObject.setProfitability(Double.valueOf(saleOffer) / Double.valueOf((vendorValue == 0 ? 1 : vendorValue)));
					flipCandidate.add(analyzedObject);
				}
			}
			i = end;
		}

		log.info("Flip candidates: ");

		for (AnalyzedObject analyzedObject : flipCandidate) {		
			System.out.println("<a href=\"" + url + analyzedObject.getDataId() + "\">" + analyzedObject.getName() + " profit ratio: " + analyzedObject.getProfitability()  + "</a></br>");
			// log.info(url + data_id);

		}
	}

}
