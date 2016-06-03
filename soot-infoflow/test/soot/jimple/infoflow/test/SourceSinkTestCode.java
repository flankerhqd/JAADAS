package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.ConnectionManager;

/**
 * Target class for the SourceSinkTests
 * 
 * @author Steven Arzt
 */
public class SourceSinkTestCode {
	
	private class Base {
		
		private String x = "foo";
		
		public String toString() {
			return x;
		}
		
	}
	
	private class A extends Base {
		private String data;
		private String data2;
		
		public A(String data) {
			this.data = data;
			this.data2 = "foo";
		}
		
		public String getData2() {
			return data2;
		}
		
	}
	
	private class B extends Base {
		
	}
	
	private A getSecret() {
		return new A("Secret");
	}
	
	private B getSecret2() {
		return new B();
	}
	
	public void testDataObject() {
		A a = getSecret();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.data);
	}
	
	private void doSomething(Object o) {
		ConnectionManager cm = new ConnectionManager();
		cm.publish("" + o);
	}
	
	public void testAccessPathTypes() {
		A a = getSecret();
		doSomething(a);
		B b = getSecret2();
		doSomething(b);
	}
	
	public void testSourceAccessPaths() {
		A a = getSecret();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.getData2());
	}
	
	private void doLeakSecret2(A a) {
		System.out.println(a.data2);
	}
	
	public void testSinkAccessPaths() {
		A a = getSecret();
		doLeakSecret2(a);
	}
	
	public void testSinkAccessPaths2() {
		A a = getSecret();
		a.data2 = a.data;
		doLeakSecret2(a);
	}

}
