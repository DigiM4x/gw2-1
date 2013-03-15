package org.salgar.igw2trade.commands;

import org.apache.commons.chain.Context;

public class MultipleLevelSearchCommand extends AbstractSearchCommand {
	protected String searchLevelMax = null;

	@Override
	protected void initialize(Context context) {
		super.initialize(context);

		searchLevelMax = (String) context.get("searchlevelMax");
	}

	@Override
	protected String constructSearchString(int offset) {
		String sqlString = "https://tradingpost-live.ncplatform.net/ws/search.json?text="
				+ "&offset=" + offset
				+ "&levelmin=" + searchLevel
				+ "&levelmax=" + searchLevelMax;
		return sqlString;
	}

}
