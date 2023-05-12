
package org.secuso.privacyfriendlyactivitytracker.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.secuso.privacyfriendlyactivitytracker.R;
import org.secuso.privacyfriendlyactivitytracker.activities.MainActivity;
import org.secuso.privacyfriendlyactivitytracker.activities.TrainingActivity;
import org.secuso.privacyfriendlyactivitytracker.models.StepCount;
import org.secuso.privacyfriendlyactivitytracker.persistence.StepCountPersistenceHelper;
import org.secuso.privacyfriendlyactivitytracker.persistence.TrainingPersistenceHelper;
import org.secuso.privacyfriendlyactivitytracker.persistence.WalkingModePersistenceHelper;
import org.secuso.privacyfriendlyactivitytracker.utils.AndroidVersionHelper;
import org.secuso.privacyfriendlyactivitytracker.utils.StepDetectionServiceHelper;
import org.secuso.privacyfriendlyactivitytracker.utils.UnitHelper;

import java.util.Calendar;
import java.util.List;


public abstract class AbstractStepDetectorService extends JobIntentService implements SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String BROADCAST_ACTION_STEPS_DETECTED = "org.secuso.privacyfriendlystepcounter.STEPS_DETECTED";
    public static final String EXTENDED_DATA_NEW_STEPS = "org.secuso.privacyfriendlystepcounter.NEW_STEPS";
    public static final String EXTENDED_DATA_TOTAL_STEPS = "org.secuso.privacyfriendlystepcounter.TOTAL_STEPS";
    public static String CHANNEL_ID = "pedometer";
    public static final int NOTIFICATION_ID = 42;
    private static final String LOG_TAG = AbstractStepDetectorService.class.getName();
    private final IBinder mBinder = new StepDetectorBinder();
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver();
    private NotificationManager mNotifyManager;
    private PowerManager.WakeLock mWakeLock;
    private int dailyStepGoal = 0;
    private int totalStepsAtLastSave = 0;
    private double totalCaloriesAtLastSave = 0;
    private double totalDistanceAtLastSave = 0;

    private int total_steps = 0;

    public AbstractStepDetectorService() {
        super();
    }

    protected void onStepDetected(int count) {
        if (count <= 0) {
            return;
        }
        this.total_steps += count;
        Log.i(LOG_TAG, count + " Step(s) detected. Steps since service start: " + this.total_steps);
        // broadcast the new steps
        Intent localIntent = new Intent(BROADCAST_ACTION_STEPS_DETECTED)
                // Add new step count
                .putExtra(EXTENDED_DATA_NEW_STEPS, count)
                .putExtra(EXTENDED_DATA_TOTAL_STEPS, total_steps);
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        // Update notification
        updateNotification();
    }

    protected Notification buildNotification(StepCount additionalStepCount) {
        int totalSteps = this.totalStepsAtLastSave + additionalStepCount.getStepCount();
        double totalDistance = this.totalDistanceAtLastSave + additionalStepCount.getDistance();
        double totalCalories = this.totalCaloriesAtLastSave + additionalStepCount.getCalories(getApplicationContext());
        // Get user preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showSteps = sharedPref.getBoolean(this.getString(R.string.pref_notification_permanent_show_steps), true);
        boolean showDistance = sharedPref.getBoolean(this.getString(R.string.pref_notification_permanent_show_distance), false);
        boolean showCalories = sharedPref.getBoolean(this.getString(R.string.pref_notification_permanent_show_calories), false);
        String message = "";
        if (showSteps) {
            message = String.format(getString(R.string.notification_text_steps), totalSteps, this.dailyStepGoal);
        }
        if (showDistance) {
            message += (!message.isEmpty()) ? "\n" : "";
            message += String.format(getString(R.string.notification_text_distance), UnitHelper.kilometerToUsersLengthUnit(UnitHelper.metersToKilometers(totalDistance), this), UnitHelper.usersLengthDescriptionShort(this));
        }
        if (showCalories) {
            message += (!message.isEmpty()) ? "\n" : "";
            message += String.format(getString(R.string.notification_text_calories), totalCalories);
        }
        if(message.isEmpty()){
            message = getString(R.string.notification_text_default);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
            mBuilder.setOnlyAlertOnce(true);
        } else {
            mBuilder = new NotificationCompat.Builder(this);
        }
        mBuilder.setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setTicker(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.app_name)))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.drawable.ic_stat_directions_walk);
        mBuilder.setContentIntent(pIntent);
        mBuilder.setProgress(this.dailyStepGoal, totalSteps, false);
        mBuilder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
        mBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        mBuilder.setSilent(true);
        return mBuilder.build();
    }

    // has to be implemented by subclasses
    @Override
    public abstract void onSensorChanged(SensorEvent event);

    protected boolean cancelNotificationOnDestroy(){
        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // currently doing nothing here.
    }

    @Override
    public void onCreate() {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification(this.stepCountFromTotalSteps()));
        super.onCreate();
        Log.i(LOG_TAG, "Creating service cycle."+ this.getClass().getName());
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "Destroying service cycle."+ this.getClass().getName());
        // release wake lock if any
        acquireOrReleaseWakeLock();
        // Unregister sensor listeners
        SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
        // Cancel notification
        if (mNotifyManager != null && cancelNotificationOnDestroy()) {
            mNotifyManager.cancel(NOTIFICATION_ID);
        }
        // Unregister shared preferences listeners
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
        // Force save of step count
        StepCountPersistenceHelper.storeStepCounts(this.mBinder, getApplicationContext(), WalkingModePersistenceHelper.getActiveMode(getApplicationContext()));
        // StepDetectionServiceHelper.startPersistenceService(this);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "Starting service cycle." + this.getClass().getName());
        acquireOrReleaseWakeLock();

        if(!StepDetectionServiceHelper.isStepDetectionEnabled(getApplicationContext())){
            stopSelf();
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean counter = Integer.valueOf(sharedPref.getString(this.getString(R.string.pref_which_step_hardware), "0")) == 0;

        // register for sensors
        if(!AndroidVersionHelper.isHardwareStepCounterEnabled(this)){
            //use accelerometer
            SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else if(counter) {
            SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Long updateInterval = Long.valueOf(sharedPref.getString(this.getString(R.string.pref_hw_background_counter_frequency), "3600000"));
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, updateInterval.intValue());
            } else {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } else {
            SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }


        // Get daily goal(s) from preferences
        String d = sharedPref.getString(getString(R.string.pref_daily_step_goal), "10000");
        this.dailyStepGoal = Integer.parseInt(d);
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        // register for steps-saved-event
        IntentFilter filterRefreshUpdate = new IntentFilter();
        filterRefreshUpdate.addAction(StepCountPersistenceHelper.BROADCAST_ACTION_STEPS_SAVED);
        filterRefreshUpdate.addAction(StepCountPersistenceHelper.BROADCAST_ACTION_STEPS_INSERTED);
        filterRefreshUpdate.addAction(StepCountPersistenceHelper.BROADCAST_ACTION_STEPS_UPDATED );
        filterRefreshUpdate.addAction(TrainingActivity.BROADCAST_ACTION_TRAINING_STOPPED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filterRefreshUpdate);
        // load step count from database
        getStepsAtLastSave();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Detect changes on preferences and update our internal variable
        if (key.equals(getString(R.string.pref_daily_step_goal))) {
            dailyStepGoal = Integer.parseInt(sharedPreferences.getString(getString(R.string.pref_daily_step_goal), "10000"));
            updateNotification();
        } else if (key.equals(getString(R.string.pref_notification_permanent_show_steps)) ||
                key.equals(getString(R.string.pref_notification_permanent_show_distance)) ||
                key.equals(getString(R.string.pref_notification_permanent_show_calories))) {
            updateNotification();
        } else if(key.equals(getString(R.string.pref_use_wake_lock))){
            acquireOrReleaseWakeLock();
        }
    }

    private void getStepsAtLastSave() {
        List<StepCount> stepCounts = StepCountPersistenceHelper.getStepCountsForDay(Calendar.getInstance(), getApplicationContext());
        totalStepsAtLastSave = 0;
        totalDistanceAtLastSave = 0;
        totalCaloriesAtLastSave = 0;
        for (StepCount stepCount : stepCounts) {
            totalStepsAtLastSave += stepCount.getStepCount();
            totalDistanceAtLastSave += stepCount.getDistance();
            totalCaloriesAtLastSave += stepCount.getCalories(getApplicationContext());
        }
    }

    protected StepCount stepCountFromTotalSteps() {
        StepCount stepCount = new StepCount();
        stepCount.setStepCount(total_steps);
        stepCount.setWalkingMode(WalkingModePersistenceHelper.getActiveMode(getApplicationContext())); // use current walking mode
        return stepCount;
    }

    protected void updateNotification() {
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(this.getString(R.string.pref_step_counter_enabled), true)){
            Notification notification = buildNotification(this.stepCountFromTotalSteps());
            mNotifyManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void acquireOrReleaseWakeLock(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useWakeLock = sharedPref.getBoolean(getString(R.string.pref_use_wake_lock), false);
        boolean useWakeLockDuringTraining = sharedPref.getBoolean(getString(R.string.pref_use_wake_lock_during_training), true);
        boolean isTrainingActive = TrainingPersistenceHelper.getActiveItem(getApplicationContext()) != null;
        if(mWakeLock == null && (useWakeLock || (useWakeLockDuringTraining && isTrainingActive))) {
            acquireWakeLock();
        }
        if(mWakeLock != null && !(useWakeLock || (useWakeLockDuringTraining && isTrainingActive))){
            releaseWakeLock();
        }
    }

    private void acquireWakeLock(){
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if(mWakeLock == null || !mWakeLock.isHeld()) {
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pfPedometer:StepDetectorWakeLock");
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock(){
        if(mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    public class StepDetectorBinder extends Binder {
        public int stepsSinceLastSave() {
            return total_steps;
        }

        public void resetStepCount() {
            total_steps = 0;
        }

        public AbstractStepDetectorService getService() {
            return AbstractStepDetectorService.this;
        }
    }

    public class BroadcastReceiver extends android.content.BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.w(LOG_TAG, "Received intent which is null.");
                return;
            }
            switch (intent.getAction()) {
                case StepCountPersistenceHelper.BROADCAST_ACTION_STEPS_INSERTED:
                case StepCountPersistenceHelper.BROADCAST_ACTION_STEPS_UPDATED:
                case StepCountPersistenceHelper.BROADCAST_ACTION_STEPS_SAVED:
                    // Steps were saved, reload step count from database
                    getStepsAtLastSave();
                    updateNotification();
                    break;
                case TrainingActivity.BROADCAST_ACTION_TRAINING_STOPPED:
                    acquireOrReleaseWakeLock();
                default:
            }
        }
    }

    void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.app_name_long);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
