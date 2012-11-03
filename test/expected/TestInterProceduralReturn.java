public class TestInterProceduralReturn {
	public void method1() {
		A a1 = method2();
		a1.g();
	}
	public A method2() {
		A a2 = new A();
		a2.f();
		return a2;
	}
}
