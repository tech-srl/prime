public class TestCallsAroundAssignment {
	public void f() {
		A a = new A();
		a.f();
		a = new A();
		a.g();
		a.h();
	}
}
