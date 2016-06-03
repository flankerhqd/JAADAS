/*
 * Copyright (c) 2015. All rights reserved. Author flankerhe@keencloudtech.
 */

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

/**
 * Created by hqd on 5/11/15.
 */
public class PendingIntentActivity extends Activity {

    public void test1()
    {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(null, 0, new Intent(), 0);
        Notification n  = new Notification.Builder(this).setContentIntent(pendingIntent).build();
    }

    public void test2()
    {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(null, 0, new Intent(), 0);
        pendingIntent.toString();
    }
}
