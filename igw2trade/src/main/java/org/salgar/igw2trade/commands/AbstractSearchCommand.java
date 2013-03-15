package org.salgar.igw2trade.commands;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.salgar.igw2trade.domain.AnalyzedObject;
import org.salgar.igw2trade.parser.SearchCommandPatterns;

public abstract class AbstractSearchCommand implements Command {
	private static final Logger log = Logger.getLogger(AbstractSearchCommand.class);
	
	protected DefaultHttpClient client = null;
	protected CookieStore cookieStore = null;
	
	protected String searchLevel = null;
	protected int minimumAmountOfDemand = 0;
	protected int profitMarginAgainstVendor = 0;
	protected int operationBracketTop = 0;
	protected int operationBracketBottom = 0;
	
	@SuppressWarnings("unchecked")
	public boolean execute(Context context) throws Exception {		
		initialize(context);
		
		String searchString = constructSearchString(1);
		
		String result = getItemList(searchString);
		
		List<AnalyzedObject> worthyObjects = new ArrayList<AnalyzedObject>();
		analyzeWorthyObjects(result, worthyObjects, minimumAmountOfDemand,
				profitMarginAgainstVendor, operationBracketTop,
				operationBracketBottom);
		
		int total = findTotalResults(result);
		for (int i = 11, n = total; i < n; i += 10) {
			searchString = constructSearchString(i);
			result = getItemList(searchString);
			analyzeWorthyObjects(result, worthyObjects, minimumAmountOfDemand,
					profitMarginAgainstVendor, operationBracketTop,
					operationBracketBottom);
		}

		context.put("worthAnalyzing", worthyObjects);
		
		return false;
	}
	
	protected abstract String constructSearchString(int offset);
	
	private int findTotalResults(String result) {
		String pattern = "\"total\":\"";
		int start = result.indexOf(pattern);
		String total = result.substring(start + pattern.length(),
				result.indexOf("\"", start + pattern.length()));

		return Integer.valueOf(total);
	}
	
	protected void initialize(Context context) {
		client = (DefaultHttpClient) context.get("client");
		cookieStore = (CookieStore) context.get("cookiestore");
		
		searchLevel = (String) context.get("searchlevel");
		minimumAmountOfDemand = (Integer) context
				.get("minimum_amount_of_demand");
		profitMarginAgainstVendor = (Integer) context
				.get("profit_margin_against_vendor_value");
		operationBracketTop = (Integer) context
				.get("operation_bracket_top");
		operationBracketBottom = (Integer) context
				.get("operation_bracket_bottom");
	}
	
	@SuppressWarnings("unused")
	private String getItemList(String searchString)
			throws ClientProtocolException, IOException {

//		HttpGet get = new HttpGet(
//				"https://tradingpost-live.ncplatform.net/ws/search.json?text="
//				/* + URLEncoder.encode("Eye of Power Scepter", "UTF-8") */
//				+ "&offset=" + offset + "&levelmin=" + searchLevel
//						+ "&levelmax=" + searchLevel);
		
		HttpGet get = new HttpGet(searchString);

		// HttpGet get = new
		// HttpGet("https://tradingpost-live.ncplatform.net/ws/search.json?ids=13716&levelmin=56&levelmax=64");

		// HttpGet get = new
		// HttpGet("https://tradingpost-live.ncplatform.net/ws/listings.json?id=13716&type=all");

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
		log.info("Got the list!");
		log.info(result);

		return result;
	}
	
	private void analyzeWorthyObjects(String result,
			List<AnalyzedObject> worthyObjects, int minimumAmountOfDemand,
			int profitMarginAgainstVendor, int operationBracketTop,
			int operationBracketBottom) {
		int i = 0;

		while ((i = result.indexOf("{\"type_id", i)) > -1) {
			int end = result.indexOf("}", i + 1);
			String object = result.substring(i, end + 1);
			AnalyzedObject analyzedObject = new AnalyzedObject();
			String[] fields = object.split(",");
			int countValue = 0;
			int sell_priceValue = 0;
			int buy_priceValue = 0;
			int vendorValue = 0;
			for (int j = 0, n = fields.length; j < n; j++) {
				if(fields[j].indexOf(SearchCommandPatterns.DATA_ID) > -1) {
					String data_id[] = fields[j].split(":");
					analyzedObject.setDataId(Integer.valueOf(data_id[1].substring(1,
							data_id[1].length() - 1)));
				} else if(fields[j].indexOf(SearchCommandPatterns.NAME) > -1) {
					String name[] = fields[j].split(":");
					analyzedObject.setName(name[1].substring(1,
							name[1].length() - 1));					
				} else if (fields[j].indexOf(SearchCommandPatterns.COUNT) > -1) {
					String[] count = fields[j].split(":");

					countValue = Integer.valueOf(count[1].substring(1,
							count[1].length() - 1));
					analyzedObject.setDemand(countValue);
				} else if (fields[j].indexOf(SearchCommandPatterns.SELL_PRICE) > -1) {
					String[] sell_price = fields[j].split(":");

					sell_priceValue = Integer.valueOf(sell_price[1].substring(1,
							sell_price[1].length() - 1));
					analyzedObject.setSalesOffer(sell_priceValue);
				} else if (fields[j].indexOf(SearchCommandPatterns.BUY_PRICE) > -1) {
					String[] buy_price = fields[j].split(":");

					buy_priceValue = Integer.valueOf(buy_price[1].substring(1,
							buy_price[1].length() - 1)); 
					analyzedObject.setBuyOffer(buy_priceValue);
				} else if(fields[j].indexOf(SearchCommandPatterns.RARITY) > -1) {
					String rarity[] = fields[j].split(":");
					
					analyzedObject.setRarity(Integer.valueOf(rarity[1].substring(1,
							rarity[1].length() - 1)));
				} else if (fields[j].indexOf(SearchCommandPatterns.VENDOR) > -1) {
					String[] vendor = fields[j].split(":");

					vendorValue = Integer.valueOf(vendor[1].substring(1,
							vendor[1].length() - 1));
					analyzedObject.setVendorValue(vendorValue);
				}
			}
			if (countValue >= minimumAmountOfDemand) {
				log.info(object);
				worthyObjects.add(analyzedObject);
			}
			i = end;
		}
	}

}
