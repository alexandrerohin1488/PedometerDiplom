
package org.secuso.privacyfriendlyactivitytracker.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.secuso.privacyfriendlyactivitytracker.models.StepCount;

import java.util.ArrayList;
import java.util.List;

/**
 * Database helper class for storing steps
 * The database stores for each entry a timestamp and the number of steps since last entry.
 */

public class StepCountDbHelper  extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 2;

    public static final String DATABASE_NAME = "StepCount.db";

    public static final String TABLE_NAME = "stepcount";

    public static final String KEY_ID = "_id";
    public static final String KEY_STEP_COUNT = "stepcount";
    public static final String KEY_WALKING_MODE = "walking_mode";
    public static final String KEY_TIMESTAMP = "timestamp";

    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY," +
                    KEY_STEP_COUNT + INTEGER_TYPE + COMMA_SEP +
                    KEY_WALKING_MODE + INTEGER_TYPE + COMMA_SEP +
                    KEY_TIMESTAMP + INTEGER_TYPE +
            " )";

    private static SQLiteDatabase db;

    private Context context;

    private static SQLiteDatabase getDatabase(StepCountDbHelper instance){
        if(db == null){
            db = instance.getWritableDatabase();
        }
        return db;
    }

    public static void invalidateReference(){
        db = null;
    }

    public StepCountDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Fill when upgrading DB
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void addStepCount(StepCount stepCount){
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(KEY_STEP_COUNT, stepCount.getStepCount());
        values.put(KEY_WALKING_MODE, (stepCount.getWalkingMode() != null) ? stepCount.getWalkingMode().getId() : 1);
        values.put(KEY_TIMESTAMP, stepCount.getEndTime());

        // Insert the new row, returning the primary key value of the new row
        getDatabase(this).insert(
                TABLE_NAME,
                null,
                values);
    }

    public void addStepCountWithID(StepCount stepCount){
        addStepCount(stepCount);
    }

    public List<StepCount> getAllStepCounts(){
        return getStepCountsForInterval(0, Long.MAX_VALUE);
    }

    public List<StepCount> getStepCountsForInterval(long start_time, long end_time) {
        Cursor c = getDatabase(this).query(TABLE_NAME,
                new String[]{
                        KEY_STEP_COUNT,
                        KEY_TIMESTAMP,
                        KEY_WALKING_MODE
                },
                KEY_TIMESTAMP + " >= ? AND " + KEY_TIMESTAMP + " <= ?", new String[]{String.valueOf(start_time),
                        String.valueOf(end_time)}, null, null, KEY_TIMESTAMP + " ASC");
        WalkingModeDbHelper walkingModeDbHelper = new WalkingModeDbHelper(context);
        List<StepCount> steps = new ArrayList<>();
        long start = start_time;
        while (c.moveToNext()) {
            StepCount s = new StepCount();
            s.setStartTime(start);
            s.setEndTime(c.getLong(c.getColumnIndexOrThrow(KEY_TIMESTAMP)));
            s.setStepCount(c.getInt(c.getColumnIndexOrThrow(KEY_STEP_COUNT)));
            s.setWalkingMode(walkingModeDbHelper.getWalkingMode(c.getInt(c.getColumnIndexOrThrow(KEY_WALKING_MODE))));
            steps.add(s);
            start = s.getEndTime();
        }
        c.close();
        return steps;
    }

    public StepCount getLatestStepCount(){
        Cursor c = getDatabase(this).query(TABLE_NAME,
                new String[]{
                        KEY_STEP_COUNT,
                        KEY_TIMESTAMP,
                        KEY_WALKING_MODE
                },
                null, null, null, null, KEY_TIMESTAMP + " DESC", "1");
        WalkingModeDbHelper walkingModeDbHelper = new WalkingModeDbHelper(context);
        StepCount s = null;
        while (c.moveToNext()) {
            s = new StepCount();
            s.setEndTime(c.getLong(c.getColumnIndexOrThrow(KEY_TIMESTAMP)));
            s.setStepCount(c.getInt(c.getColumnIndexOrThrow(KEY_STEP_COUNT)));
            s.setWalkingMode(walkingModeDbHelper.getWalkingMode(c.getInt(c.getColumnIndexOrThrow(KEY_WALKING_MODE))));
        }
        c.close();
        return s;
    }

    public StepCount getFirstStepCount(){
        Cursor c = getDatabase(this).query(TABLE_NAME,
                new String[]{
                        KEY_STEP_COUNT,
                        KEY_TIMESTAMP,
                        KEY_WALKING_MODE
                },
                null, null, null, null, KEY_TIMESTAMP + " ASC", "1");
        WalkingModeDbHelper walkingModeDbHelper = new WalkingModeDbHelper(context);
        StepCount s = null;
        while (c.moveToNext()) {
            s = new StepCount();
            s.setEndTime(c.getLong(c.getColumnIndexOrThrow(KEY_TIMESTAMP)));
            s.setStepCount(c.getInt(c.getColumnIndexOrThrow(KEY_STEP_COUNT)));
            s.setWalkingMode(walkingModeDbHelper.getWalkingMode(c.getInt(c.getColumnIndexOrThrow(KEY_WALKING_MODE))));
        }
        c.close();
        return s;
    }

    public void updateStepCount(StepCount stepCount) {
        this.updateStepCount(stepCount, stepCount.getEndTime());
    }

    public void updateStepCount(StepCount stepCount, long oldEndTime){
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(KEY_STEP_COUNT, stepCount.getStepCount());
        values.put(KEY_WALKING_MODE, (stepCount.getWalkingMode() != null) ? stepCount.getWalkingMode().getId() : 1);
        values.put(KEY_TIMESTAMP, stepCount.getEndTime());

        // Update the row, returning the primary key value of the new row
        getDatabase(this).update(
                TABLE_NAME,
                values,
                KEY_TIMESTAMP + " = ?",
                new String[]{String.valueOf(oldEndTime)}
        );
    }

    public void deleteStepCount(StepCount stepCount){
        String selection = KEY_TIMESTAMP + " = ?";
        String[] selectionArgs = {String.valueOf(stepCount.getEndTime())};
        this.getDatabase(this).delete(TABLE_NAME, selection, selectionArgs);
    }

    public void deleteAllStepCounts(){
        getDatabase(this).execSQL("delete from " + TABLE_NAME);
    }

    public static abstract class StepCountEntry implements BaseColumns {
        public static final String TABLE_NAME = StepCountDbHelper.TABLE_NAME;
        public static final String KEY_STEP_COUNT = StepCountDbHelper.KEY_STEP_COUNT;
        public static final String KEY_WALKING_MODE = StepCountDbHelper.KEY_WALKING_MODE;
        public static final String KEY_TIMESTAMP = StepCountDbHelper.KEY_TIMESTAMP;
    }


}
