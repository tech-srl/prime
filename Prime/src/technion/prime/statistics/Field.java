package technion.prime.statistics;

public class Field {
	private String title;
	private Class<?> type;
	
	protected Field(String title, Class<?> type) {
		this.title = title;
		this.type = type;
	}

	public Class<?> getType() {
		return type;
	}
	
	public String getTitle() {
		return title;
	}
	
	public Object getDefault() {
		if (type == String.class) return "";
		if (type == Integer.class) return new Integer(0);
		if (type == Long.class)return new Long(0);
		if (type == Double.class) return new Double(0);
		return null;
	}
}
