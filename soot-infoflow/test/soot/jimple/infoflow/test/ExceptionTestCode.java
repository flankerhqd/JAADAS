package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Tests for exceptional data- and control flows
 * 
 * @author Steven Arzt
 */
public class ExceptionTestCode {
	
	public void exceptionControlFlowTest1() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			doThrowException();
		}
		catch (RuntimeException ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(tainted);
			System.out.println(ex);
		}
	}

	private void doThrowException() {
		throw new RuntimeException("foo");
	}
	
	public void exceptionControlFlowTest2() {
		try {
			String s = getConstantStringAndThrow();
			System.out.println(s);
		}
		catch (Exception ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(TelephonyManager.getDeviceId());
			System.out.println(ex);
		}
	}
	
	private String getConstantStringAndThrow() {
		throw new RuntimeException("foo");
	}
	
	public void exceptionControlFlowTest3() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			tainted = doThrowImplicitException();
		}
		catch (ArrayIndexOutOfBoundsException ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(tainted);
			System.out.println(ex);
		}
		System.out.println(tainted);
	}
	
	private String doThrowImplicitException() {
		String[] foo = new String[2];
		foo[10] = "Hello World";
		return "foo";
	}
	
	public void exceptionDataFlowTest1() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			throwData(tainted);
		}
		catch (Exception ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(ex.getMessage());
		}
	}

	private void throwData(String tainted) {
		throw new RuntimeException(tainted);
	}
	
	public void exceptionDataFlowTest2() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			throw new RuntimeException(tainted);
		}
		catch (Exception ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(ex.getMessage());
		}
	}

}
