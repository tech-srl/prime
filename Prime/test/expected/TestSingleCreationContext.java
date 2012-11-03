public class TestSingleCreationContext {
	public void f() {
		B b = new B();
		b.f();
		A a = b.g();
		b.ignored();
		a.h();
	}
}
