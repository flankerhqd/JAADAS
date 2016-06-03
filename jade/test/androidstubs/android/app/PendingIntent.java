package android.app;

import android.content.Context;
import android.content.Intent;

/**
 * Created by hqd on 5/5/15.
 */
public class PendingIntent {
    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags)
    {
        return new PendingIntent();
    }

    public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags)
    {
        return new PendingIntent();
    }

    public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags)
    {
        return new PendingIntent();
    }

    public static PendingIntent getActivities(Context context, int requestCode, Intent intent, int flags)
    {
        return new PendingIntent();
    }
}
