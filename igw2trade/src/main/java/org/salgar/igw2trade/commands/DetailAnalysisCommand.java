package org.salgar.igw2trade.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.log4j.Logger;
import org.salgar.igw2trade.domain.AnalyzedObject;

public class DetailAnalysisCommand implements Command {
	private static final Logger log = Logger
			.getLogger(DetailAnalysisCommand.class);

	public boolean execute(Context context) throws Exception {
		String url = (String) context.get("guildwarstrade_url");
		Double ratioOfBuyOfferToSellOfferValue = Double.valueOf(context
				.get("ratio_of_buy_offer_to_sell_offer").toString());
		int minimumBuyDemand = (Integer) context.get("minimum_buy_demand");
		int maximumBuyOffer = (Integer) context.get("operation_bracket_top");
		int rarity = (Integer) context.get("rarity");

		@SuppressWarnings("unchecked")
		List<AnalyzedObject> worthAnalyzing = (List<AnalyzedObject>) context
				.get("worthAnalyzing");

		
			analyze(worthAnalyzing, url,
					ratioOfBuyOfferToSellOfferValue, minimumBuyDemand, maximumBuyOffer, rarity);
		return false;
	}

	private void analyze(List<AnalyzedObject> worthAnalyzing, String url, Double ratioOfBuyOfferToSellOfferValue, 
			int minimumBuyDemand, int maximumBuyOffer, int rarity) throws IOException {
		
		List<AnalyzedObject> flipCandidate = new ArrayList<AnalyzedObject>();
		for (AnalyzedObject analyzedObject : worthAnalyzing) {
			
			if (analyzedObject.getSalesOffer() >= ratioOfBuyOfferToSellOfferValue * analyzedObject.getBuyOffer()
					&& analyzedObject.getDemand() >= minimumBuyDemand && analyzedObject.getRarity() >= rarity) {
				if (analyzedObject.getBuyOffer() >= analyzedObject.getVendorValue() && analyzedObject.getBuyOffer() <= maximumBuyOffer) {					
					analyzedObject.setProfitability(Double.valueOf(analyzedObject.getSalesOffer()) / Double.valueOf(analyzedObject.getBuyOffer()));
					flipCandidate.add(analyzedObject);
				} else if ((analyzedObject.getSalesOffer() >= ratioOfBuyOfferToSellOfferValue
						* (analyzedObject.getVendorValue() == 0 ? 1 : analyzedObject.getVendorValue())) && (analyzedObject.getVendorValue() <= maximumBuyOffer) ) {
					analyzedObject.setProfitability(Double.valueOf(analyzedObject.getSalesOffer()) / Double.valueOf((analyzedObject.getVendorValue() == 0 ? 1 : analyzedObject.getVendorValue())));
					flipCandidate.add(analyzedObject);
				}
			}
		}

		log.info("Flip candidates: ");

		for (AnalyzedObject analyzedObject : flipCandidate) {		
			System.out.println("<a href=\"" + url + analyzedObject.getDataId() + "\">" + analyzedObject.getName() + " profit ratio: " + analyzedObject.getProfitability()  + "</a></br>");
			// log.info(url + data_id);

		}
	}

}
