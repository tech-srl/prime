package technion.prime.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import technion.prime.utils.Logger.CanceledException;

public class HistoryTestUtils {
	public static void assertEqualContentHistories(HistoryCollection hc, History... expected)
			throws InterruptedException, CanceledException {
		assertEquals(expected.length, hc.getNumHistories());
		for (History h1 : expected) {
			History matched = null;
			for (History h2 : hc.getHistories()) {
				if (h1.equalContent(h2)) {
					matched = h2;
					break;
				}
			}
			assertNotNull(matched);
			hc.removeHistory(matched);
		}
	}
}
