package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Created by hqd on 1/29/15.
 */
public class ProblemTestCode {
    public static class TestCase {
        private static String secret = TelephonyManager.getDeviceId();
        private static int zero = zero();
        private static int zero() {
            return 0;
        }

        public static String getSecret()
        {
            return secret;
        }
    }

    public void static5Test(){
        ConnectionManager cm = new ConnectionManager();
        cm.publish(TestCase.getSecret());
    }
}

