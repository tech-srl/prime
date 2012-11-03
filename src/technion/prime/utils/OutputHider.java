package technion.prime.utils;

import java.io.OutputStream;
import java.io.PrintStream;

public class OutputHider {
	private static final PrintStream nullOutputStream = new PrintStream(new OutputStream() {
		@Override public void write(int b) {}
	});
	
	private final PrintStream s;
	
	public OutputHider() {
		s = System.out;
		System.setOut(nullOutputStream);
	}
	
	public void release() {
		System.setOut(s);
	}
}
