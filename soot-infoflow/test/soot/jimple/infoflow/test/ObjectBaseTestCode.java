package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.Activity;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Created by hqd on 3/10/15.
 */
public class ObjectBaseTestCode {
    public class Y{
        String f;

        public void set(String s){
            f = s;
        }
    }

    public void simpleTestBaseActivity(){
        String taint = TelephonyManager.getDeviceId();
        Activity activity = new Activity();
        activity.setTainted(taint);
        activity.startActivity();
    }
}
