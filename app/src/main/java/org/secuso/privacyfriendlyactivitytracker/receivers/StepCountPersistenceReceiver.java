package org.secuso.privacyfriendlyactivitytracker.receivers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.legacy.content.WakefulBroadcastReceiver;

import org.secuso.privacyfriendlyactivitytracker.Factory;
import org.secuso.privacyfriendlyactivitytracker.models.WalkingMode;
import org.secuso.privacyfriendlyactivitytracker.persistence.StepCountPersistenceHelper;
import org.secuso.privacyfriendlyactivitytracker.persistence.WalkingModePersistenceHelper;
import org.secuso.privacyfriendlyactivitytracker.utils.StepDetectionServiceHelper;

public class StepCountPersistenceReceiver extends WakefulBroadcastReceiver {
    private static final String LOG_CLASS = StepCountPersistenceReceiver.class.getName();
    private WalkingMode oldWalkingMode;
    private Context context;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepCountPersistenceHelper.storeStepCounts(service, context, oldWalkingMode);
            context.getApplicationContext().unbindService(mServiceConnection);
            Log.d("service cycle", "UNbound service in onServiceConnected PERSISTENCEReceiver");

            StepDetectionServiceHelper.stopAllIfNotRequired(false, context);
            WidgetReceiver.forceWidgetUpdate(context);
            if(mSaveListener != null) {
                mSaveListener.onSaveDone();
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(LOG_CLASS, "Storing the steps");
        this.context = context.getApplicationContext();
        if (intent.hasExtra(WalkingModePersistenceHelper.BROADCAST_EXTRA_OLD_WALKING_MODE)) {
            oldWalkingMode = WalkingModePersistenceHelper.getItem(intent.getLongExtra(WalkingModePersistenceHelper.BROADCAST_EXTRA_OLD_WALKING_MODE, -1), context);
        }
        if(oldWalkingMode == null){
            oldWalkingMode = WalkingModePersistenceHelper.getActiveMode(context);
        }
        // bind to service
        Intent serviceIntent = new Intent(context, Factory.getStepDetectorServiceClass(context));
        context.getApplicationContext().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        Log.d("service cycle", "bound service in onReceive PERSISTENCEReceiver");

    }

    public interface ISaveListener {
        void onSaveDone();
    }

    private static ISaveListener mSaveListener = null;

    public static void registerSaveListener(ISaveListener listener) {
        mSaveListener = listener;
    }

    public static void unregisterSaveListener() {
        mSaveListener = null;
    }


}
