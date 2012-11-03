class TestLoop {
	public void f(B b) {
		A a = new A();
		a.f();
		while(b.something()) {
			a.g();
		}
		a.h();
	}
}