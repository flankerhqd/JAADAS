import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;

/**
 * Created by hqd on 3/13/15.
 */
public class TestActivity extends Activity {

    public void test1()
    {
        //implicit
        Intent intent = new Intent();
        startActivity(intent);
    }

    public void test2()
    {
        //explicit
        Intent intent = new Intent();
        intent.setComponent(new ComponentName());
        startActivity(intent);
    }

    public void test3()
    {
        //explicit
        Intent intent = new Intent(this, TestActivity.class);
        startActivity(intent);
    }

    public void test4()
    {
        //explicit, because we consider all paths
        Intent intent = new Intent(this, TestActivity.class);
        if(Runtime.getRuntime().availableProcessors() == 1)
        {
            intent.setClass(this, TestActivity.class);
        }
        startActivity(intent);
    }

    public void test5()
    {
        //implicit
        Intent intent = new Intent();
        startActivity(intent);

        //explicit
        intent.setClass(this, TestActivity.class);
        startActivity(intent);
    }


    public void test6()
    {
        //implicit
        Intent intent = new Intent(this, TestActivity.class);
        startActivity(intent);

        intent = new Intent();
        intent.setClass(this, TestActivity.class);
        startActivity(intent);
    }

    Intent intent;
    public void test7()
    {
        //field test
        //explicit
        intent = new Intent();
        intent.setClass(this, TestActivity.class);
        startActivity(intent);
        //intent = new Intent(this, TestActivity.class);
        //startActivity(intent);
    }

    public void test8()
    {
        //field test
        //implicit
        intent = new Intent();
        //intent.setClass(this, TestActivity.class);
        startActivity(intent);
        //intent = new Intent(this, TestActivity.class);
        //startActivity(intent);
    }

    public void test9()
    {
        //field test
        //implicit
        intent = new Intent();
        startActivity(intent);
        //intent = new Intent(this, TestActivity.class);
        intent = new Intent();
        intent.setClass(this, TestActivity.class);
        startActivity(intent);
    }
}
