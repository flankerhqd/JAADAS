package heros.util;

public class SootThreadGroup extends ThreadGroup {

	private Thread startThread;

	public SootThreadGroup() {
		super("Soot Threadgroup");
		if (Thread.currentThread().getThreadGroup() instanceof SootThreadGroup) {
			SootThreadGroup group = (SootThreadGroup) Thread.currentThread().getThreadGroup();
			startThread = group.getStarterThread();
		} else {
			startThread = Thread.currentThread();
		}
	}

	public Thread getStarterThread() {
		return startThread;
	}
}
