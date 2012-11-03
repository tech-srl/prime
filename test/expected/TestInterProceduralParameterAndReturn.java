public class TestInterProceduralParameterAndReturn {
	public void method1() {
		A a1 = new A();
		a1.f();
		A a3 = method2(a1);
		a3.h();
	}
	public A method2(A a2) {
		a2.g();
		return a2;
	}
}
