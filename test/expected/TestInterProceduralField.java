public class TestInterProceduralField {
	A a;
	public void method1() {
		a.f();
		method2();
		a.h();
	}
	public void method2() {
		a.g();
	}
}
