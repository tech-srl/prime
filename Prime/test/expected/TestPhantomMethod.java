public class TestPhantomMethod extends Something {
	public void f() {
		A a = new A();
		a.f();
		transmogrify(a);
		a.g();
	}
}
