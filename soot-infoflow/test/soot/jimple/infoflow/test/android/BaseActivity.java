package soot.jimple.infoflow.test.android;

/**
 * Created by hqd on 3/10/15.
 */
public class BaseActivity extends Context{

    public String getTainted() {
        return tainted;
    }

    public void setTainted(String tainted) {
        this.tainted = tainted;
    }

    private String tainted = "";
}
