public class TestAssignmentFromOpaque {
	public void f() {
		A a = new A();
		a.f();
		a = a.copy();
		a.g();
	}
}
