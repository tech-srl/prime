class TestLoop {
	public void f() {
		A a = new A();
		a.f();
		while(a.condition()) {
			a.g();
		}
		a.h();
	}
}
