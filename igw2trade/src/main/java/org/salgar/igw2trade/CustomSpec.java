package org.salgar.igw2trade;

import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.cookie.BasicExpiresHandler;
import org.apache.http.impl.cookie.NetscapeDraftSpec;

public class CustomSpec extends NetscapeDraftSpec {
	private final String[] datepatterns;

	public CustomSpec() {
		super();
		datepatterns = new String[] { EXPIRES_PATTERN,
				"EEE, dd MMM yyyy HH:mm:ss z" };
		registerAttribHandler(ClientCookie.EXPIRES_ATTR,
				new BasicExpiresHandler(this.datepatterns));
	}
	
	@Override
	public String toString() {
		return "custom";
	}
}
