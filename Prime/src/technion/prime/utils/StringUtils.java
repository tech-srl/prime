package technion.prime.utils;

public class StringUtils {
	public static String join(Iterable<String> strings, String delimiter) {
		if (strings == null) return "";
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String s : strings) {
			if (first == false) sb.append(delimiter);
			sb.append(s);
			first = false;
		}
		return sb.toString();
	}
	
	public static String join(String[] strings, String delimiter) {
		if (strings == null) return "";
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String s : strings) {
			if (first == false) sb.append(delimiter);
			sb.append(s);
			first = false;
		}
		return sb.toString();
	}
	
	/**
	 * Returns a string in which <code>s</code> is repeated <code>times</code> times.
	 * If <code>times</code> is zero or negative, the empty string is returned.
	 */
	public static String repeat(String s, int times) {
		if (times <= 0) return "";
		StringBuilder sb = new StringBuilder(s.length() * times);
		for (int i = 0 ; i < times ; i++) sb.append(s);
		return sb.toString();
	}
	
	public static String normalizeTo(String s, int size) {
		if (s.length() <= size) {
			for (int i = 0 ; i < s.length() - size ; i++) {
				s += " ";
			}
			return s;
		}
		return "..." + s.substring(s.length() - size + 3, s.length());
	}
	
	public static String prettyPrintNumber(double d) {
		if (d < 0) return String.valueOf(d);
		
		String[] suffixes = new String[] {
				"", "k", "M", "G", "T"
		};
		int i;
		for (i = suffixes.length - 1; i > 0; i--) {
			int threshold = (int)Math.pow(10, i*3);
			if (d >= threshold) {
				d /= threshold;
				break;
			}
		}
		if (i > 0) d = Math.round(d);
		String decimalStr = "";
		if (d >= 100) {
			d = Math.round(d); // No more than 3 significant digits
		} else {
			long decimal = (((long)(d * 10)) % 10);
			decimalStr = "" + (decimal == 0 ? "" : "." + decimal);
		}
		return "" + ((long)d) + decimalStr + suffixes[i];
	}

}
