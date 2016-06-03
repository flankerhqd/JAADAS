package soot.jimple.infoflow.android.manifest;

import java.util.List;

/**
 * Created by hqd on 2/4/15.
 */
public class IntentFilter {

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }


    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    private List<String> actions;
    private List<String> categories;
}
