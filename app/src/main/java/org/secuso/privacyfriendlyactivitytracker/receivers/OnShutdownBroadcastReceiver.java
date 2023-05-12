package org.secuso.privacyfriendlyactivitytracker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.secuso.privacyfriendlyactivitytracker.utils.StepDetectionServiceHelper;

public class OnShutdownBroadcastReceiver extends BroadcastReceiver {
    private static final String LOG_CLASS = OnShutdownBroadcastReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(LOG_CLASS, "onReceive");
        StepDetectionServiceHelper.startPersistenceService(context);
    }
}
