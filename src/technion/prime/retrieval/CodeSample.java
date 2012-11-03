package technion.prime.retrieval;

import technion.prime.utils.Logger.CanceledException;

public interface CodeSample {
	String getFilename() throws CanceledException, InterruptedException;
}
