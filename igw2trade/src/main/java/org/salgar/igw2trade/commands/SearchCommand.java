package org.salgar.igw2trade.commands;

public class SearchCommand extends AbstractSearchCommand {

	@Override
	protected String constructSearchString(int offset) {
		String sqlString = "https://tradingpost-live.ncplatform.net/ws/search.json?text="
			+ "&offset=" + offset + "&levelmin=" + searchLevel
					+ "&levelmax=" + searchLevel;
		return sqlString;
	}

}
