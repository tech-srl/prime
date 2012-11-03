package technion.prime.retrieval;

public class LocalCodeSample implements CodeSample {

	private String filename;

	public LocalCodeSample(String filename) {
		this.filename = filename;
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.retrieval.CodeSample#getFilename()
	 */
	@Override
	public String getFilename() {
		return filename;
	}

}
