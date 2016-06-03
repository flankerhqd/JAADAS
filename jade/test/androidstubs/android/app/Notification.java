/*
 * Copyright (c) 2015. All rights reserved. Author flankerhe@keencloudtech.
 */

package android.app;

import android.content.Context;
/**
 * Created by hqd on 5/11/15.
 */
public class Notification {
    private PendingIntent pendingIntent;
    static public class Builder{
        Context context;
        public Builder(Context context)
        {
            this.context = context;
        }
        public Notification build()
        {
            return new Notification(pendingIntent);
        }

        PendingIntent pendingIntent;
        public Builder setContentIntent(PendingIntent intent)
        {
            this.pendingIntent = intent;
            return this;
        }
    }

    private Notification(PendingIntent pendingIntent)
    {
        this.pendingIntent = pendingIntent;
    }

}
