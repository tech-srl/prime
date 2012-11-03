public class TestFocus {
	public void method(boolean flag) {
		A a1 = new A();
		a1.f1();
		A a2 = new A();
		a2.f2();
		if (flag) {
			a2 = a1;
		}
		a2.g();
		a2.h();
	}
}
