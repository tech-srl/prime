public class TestStaticCall {
	public void f() {
		A a = new A();
		a.f();
		A.staticMethod(a);
		a.g();
	}
}
