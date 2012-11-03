package technion.prime.utils;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * An immutable filter that either passes or fails strings based on input patterns.
 */
public class StringFilter implements Serializable {
	private static final long serialVersionUID = 3648615930394379959L;
	
	public static final Pattern PATTERN_MATCH_ALL = Pattern.compile(".*");
	public static final Pattern PATTERN_MATCH_NONE = Pattern.compile("^$");
	public static final StringFilter ALWAYS_PASSING =
		new StringFilter(PATTERN_MATCH_ALL, PATTERN_MATCH_NONE, true, true);
	
	private final Pattern include;
	private final Pattern exclude;
	private final boolean passIfBoth;
	private final boolean passIfNone;

	/**
	 * Creates a new string filter.
	 * @param include Pattern of strings that will pass the filter.
	 * @param exclude Pattern of strings that will fail the filter.
	 * @param passIfBoth Whether the filter passes a string if it matches both the include and exclude patterns.
	 * @param passIfNone Whether the filter passes a string if it matches neither the include nor the exclude patterns.
	 */
	public StringFilter(Pattern include, Pattern exclude, boolean passIfBoth, boolean passIfNone) {
		this.include = include;
		this.exclude = exclude;
		this.passIfBoth = passIfBoth;
		this.passIfNone = passIfNone;
	}
	
	/**
	 * @return True if the string does not pass the filter.
	 */
	public boolean failsFilter(String s) {
		return !passesFilter(s);
	}
	
	/**
	 * @return True if the string passes the filter.
	 */
	public boolean passesFilter(String s) {
		boolean included = include.matcher(s).matches();
		boolean excluded = exclude.matcher(s).matches();
		if (included  && !excluded)     return true;
		if (!included && excluded)      return false;
		if (included  && excluded)      return passIfBoth;
		/*if (!included && !excluded)*/ return passIfNone;
	}

	@Override
	public String toString() {
		return String.format("including \"%s\", excluding \"%s\", passing if both: %s, passing if none: %s",
				include.toString(), exclude.toString(), String.valueOf(passIfBoth), String.valueOf(passIfNone));
	}
	
}
