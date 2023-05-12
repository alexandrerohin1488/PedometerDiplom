
package org.secuso.privacyfriendlyactivitytracker.persistence;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.secuso.privacyfriendlyactivitytracker.R;
import org.secuso.privacyfriendlyactivitytracker.models.StepCount;
import org.secuso.privacyfriendlyactivitytracker.models.WalkingMode;
import org.secuso.privacyfriendlyactivitytracker.services.AbstractStepDetectorService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StepCountPersistenceHelper {

    public static final String BROADCAST_ACTION_STEPS_SAVED = "org.secuso.privacyfriendlystepcounter.STEPS_SAVED";
    public static final String BROADCAST_ACTION_STEPS_UPDATED = "org.secuso.privacyfriendlystepcounter.STEPS_UPDATED";
    public static final String BROADCAST_ACTION_STEPS_INSERTED = "org.secuso.privacyfriendlystepcounter.STEPS_INSERTED";
    public static String LOG_CLASS = StepCountPersistenceHelper.class.getName();
    private static SQLiteDatabase db = null;

    public static boolean storeStepCounts(IBinder serviceBinder, Context context, WalkingMode walkingMode) {
        if (serviceBinder == null) {
            Log.e(LOG_CLASS, "Cannot store step count because service binder is null.");
            return false;
        }
        AbstractStepDetectorService.StepDetectorBinder myBinder = (AbstractStepDetectorService.StepDetectorBinder) serviceBinder;
        StepCountDbHelper stepCountDbHelper = new StepCountDbHelper(context);

        // Get the steps since last save
        int stepCountSinceLastSave = myBinder.stepsSinceLastSave();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        long updateInterval = Long.parseLong(sharedPref.getString(context.getString(R.string.pref_hw_background_counter_frequency), "3600000"));
        StepCount lastStoredStepCount = stepCountDbHelper.getLatestStepCount();;
        long currentTime = Calendar.getInstance().getTimeInMillis();
        long currentUpdateIntervalStartTime = currentTime - (updateInterval > 0 ? currentTime % updateInterval : 0);
        if(lastStoredStepCount == null || (lastStoredStepCount.getEndTime() < currentUpdateIntervalStartTime && stepCountSinceLastSave + lastStoredStepCount.getStepCount() > 0) ||
                lastStoredStepCount.getWalkingMode() != null && walkingMode != null && walkingMode.getId() != lastStoredStepCount.getWalkingMode().getId()) {
            // create new step count if none is stored or last one was saved before the current update interval and there are new staps to save
            // (the time interval of the previous step count may only be extended if it had 0 steps and there are 0 steps to add)
            StepCount stepCount = new StepCount();
            stepCount.setWalkingMode(walkingMode);
            stepCount.setStepCount(stepCountSinceLastSave);
            stepCount.setEndTime(currentTime);
            stepCountDbHelper.addStepCount(stepCount);
            Log.i(LOG_CLASS, "Creating new step count");
        } else {
            lastStoredStepCount.setStepCount(lastStoredStepCount.getStepCount() + stepCountSinceLastSave);
            long oldEndTime = lastStoredStepCount.getEndTime();
            lastStoredStepCount.setEndTime(currentTime);
            stepCountDbHelper.updateStepCount(lastStoredStepCount, oldEndTime);
            Log.i(LOG_CLASS, "Updating last stored step count - not creating a new one");
        }
        // reset step count
        myBinder.resetStepCount();
        Log.i(LOG_CLASS, "Stored " + stepCountSinceLastSave + " steps");

        // broadcast the event
        Intent localIntent = new Intent(BROADCAST_ACTION_STEPS_SAVED);
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

        return true;
    }

    public static boolean storeStepCount(StepCount stepCount, Context context) {
        new StepCountDbHelper(context).addStepCount(stepCount);

        // broadcast the event
        Intent localIntent = new Intent(BROADCAST_ACTION_STEPS_INSERTED);
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
        return true;
    }

    public static boolean updateStepCount(StepCount stepCount, Context context) {
        new StepCountDbHelper(context).updateStepCount(stepCount);
        // broadcast the event
        Intent localIntent = new Intent(BROADCAST_ACTION_STEPS_UPDATED);
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
        return true;
    }

    public static int getStepCountForDay(Calendar calendar, Context context) {
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        long start_time = calendar.getTimeInMillis();
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        long end_time = calendar.getTimeInMillis();
        return StepCountPersistenceHelper.getStepCountForInterval(start_time, end_time, context);
    }

    public static List<StepCount> getStepCountsForDay(Calendar calendar, Context context) {
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        long start_time = calendar.getTimeInMillis();
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        long end_time = calendar.getTimeInMillis();
        return StepCountPersistenceHelper.getStepCountsForInterval(start_time, end_time, context);
    }

    public static List<StepCount> getStepCountsForInterval(long start_time, long end_time, Context context) {
        if (context == null) {
            Log.e(LOG_CLASS, "Cannot get step count - context is null");
            return new ArrayList<>();
        }
        return new StepCountDbHelper(context).getStepCountsForInterval(start_time, end_time);
    }

    public static List<StepCount> getStepCountsForever(Context context) {
        if (context == null) {
            Log.e(LOG_CLASS, "Cannot get step count - context is null");
            return new ArrayList<>();
        }
        Cursor c = getDB(context).query(StepCountDbHelper.StepCountEntry.TABLE_NAME,
                new String[]{StepCountDbHelper.StepCountEntry.KEY_STEP_COUNT, StepCountDbHelper.StepCountEntry.KEY_TIMESTAMP, StepCountDbHelper.StepCountEntry.KEY_WALKING_MODE},
                "", new String[]{}, null, null, StepCountDbHelper.StepCountEntry.KEY_TIMESTAMP + " ASC");
        List<StepCount> steps = new ArrayList<>();
        long start = 0;
        int sum = 0;
        while (c.moveToNext()) {
            StepCount s = new StepCount();
            s.setStartTime(start);
            s.setEndTime(c.getLong(c.getColumnIndexOrThrow(StepCountDbHelper.StepCountEntry.KEY_TIMESTAMP)));
            s.setStepCount(c.getInt(c.getColumnIndexOrThrow(StepCountDbHelper.StepCountEntry.KEY_STEP_COUNT)));
            //Log.w("ASDF", "Getting walking mode " + c.getLong(c.getColumnIndexOrThrow(StepCountDbHelper.StepCountEntry.COLUMN_NAME_WALKING_MODE)));
            s.setWalkingMode(WalkingModePersistenceHelper.getItem(c.getLong(c.getColumnIndexOrThrow(StepCountDbHelper.StepCountEntry.KEY_WALKING_MODE)), context));
            steps.add(s);
            start = s.getEndTime();
            sum += s.getStepCount();
        }
        c.close();
        return steps;
    }

    public static int getStepCountForInterval(long start_time, long end_time, Context context) {
        int steps = 0;
        for (StepCount s : getStepCountsForInterval(start_time, end_time, context)) {
            steps += s.getStepCount();
        }
        return steps;
    }

    public static Date getDateOfFirstEntry(Context context){
        StepCount s = new StepCountDbHelper(context).getFirstStepCount();
        Date date = Calendar.getInstance().getTime(); // fallback is today
        if(s != null){
            date.setTime(s.getEndTime());
        }
        return date;
    }

    public static StepCount getLastStepCountEntryForDay(Calendar day, Context context){
        List<StepCount> stepCounts = getStepCountsForDay(day, context);
        if(stepCounts.size() == 0){
            return null;
        }else{
            return stepCounts.get(stepCounts.size() - 1);
        }
    }

    protected static SQLiteDatabase getDB(Context context) {
        if (StepCountPersistenceHelper.db == null) {
            StepCountDbHelper dbHelper = new StepCountDbHelper(context);
            StepCountPersistenceHelper.db = dbHelper.getWritableDatabase();
        }
        return StepCountPersistenceHelper.db;
    }
}
