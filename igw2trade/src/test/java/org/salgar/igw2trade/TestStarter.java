package org.salgar.igw2trade;

import org.salgar.igw2trade.runner.ChainRunner;
import org.testng.annotations.Test;

@Test
public class TestStarter {
	@Test
	public void AuthenticationTest() throws Throwable {
		ChainRunner runner = new ChainRunner();
		runner.runChain();
	}
}
