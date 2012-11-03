public class TestInterProceduralParameter {
	public void method1() {
		A a = new A();
		a.f();
		method2(a);
		a.h();
	}
	public void method2(A a2) {
		a2.g();
	}
}
