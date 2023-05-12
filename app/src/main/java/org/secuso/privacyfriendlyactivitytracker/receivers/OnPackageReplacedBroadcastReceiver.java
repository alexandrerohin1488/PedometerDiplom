package org.secuso.privacyfriendlyactivitytracker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import org.secuso.privacyfriendlyactivitytracker.R;
import org.secuso.privacyfriendlyactivitytracker.utils.StepDetectionServiceHelper;

public class OnPackageReplacedBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // init preferences
        PreferenceManager.setDefaultValues(context, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(context, R.xml.pref_notification, false);

        // start all services
        StepDetectionServiceHelper.startAllIfEnabled(context);
    }
}
