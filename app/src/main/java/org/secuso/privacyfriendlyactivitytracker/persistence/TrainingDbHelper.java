package org.secuso.privacyfriendlyactivitytracker.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.secuso.privacyfriendlyactivitytracker.models.Training;

import java.util.ArrayList;
import java.util.List;

public class TrainingDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "TrainingSessions.db";

    public static final String TABLE_NAME = "walkingmodes";

    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_STEPS = "steps";
    public static final String KEY_CALORIES = "calories";
    public static final String KEY_DISTANCE = "distance";
    public static final String KEY_START = "start";
    public static final String KEY_END = "end";
    public static final String KEY_FEELING = "feeling";

    private static final String STRING_TYPE = " TEXT";
    private static final String REAL_TYPE = " REAL";

    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY," +
                    KEY_NAME + STRING_TYPE + COMMA_SEP +
                    KEY_DESCRIPTION + STRING_TYPE + COMMA_SEP +
                    KEY_STEPS + REAL_TYPE + COMMA_SEP +
                    KEY_DISTANCE + REAL_TYPE + COMMA_SEP +
                    KEY_CALORIES + REAL_TYPE + COMMA_SEP +
                    KEY_START + REAL_TYPE + COMMA_SEP +
                    KEY_END + REAL_TYPE + COMMA_SEP +
                    KEY_FEELING + REAL_TYPE +
            " )";
    private static SQLiteDatabase db;

    private static SQLiteDatabase getDatabase(TrainingDbHelper instance){
        if(db == null){
            db = instance.getWritableDatabase();
        }
        return db;
    }

    public static void invalidateReference(){
        db = null;
    }

    public TrainingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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

    protected long addTraining(Training item) {
        ContentValues values = item.toContentValues();
        return getDatabase(this).insert(
                TABLE_NAME,
                null,
                values);
    }

    protected long addTrainingWithID(Training item) {
        ContentValues values = item.toContentValues();
        values.put(KEY_ID, item.getId());
        return getDatabase(this).insert(
                TABLE_NAME,
                null,
                values);
    }

    public Training getTraining(int id) {
        Cursor c = getCursor(KEY_ID + " = ?", new String[]{String.valueOf(id)});
        Training trainingSession;
        if (c == null) {
            return null;
        }
        if (c.getCount() == 0) {
            trainingSession = null;
        } else {
            c.moveToFirst();
            trainingSession = Training.from(c);
        }

        c.close();
        return trainingSession;
    }

    public Training getActiveTraining() {
        Cursor c = getCursor(KEY_END + " = ?", new String[]{"0"});
        Training trainingSession;
        if (c == null) {
            return null;
        }
        if (c.getCount() == 0) {
            trainingSession = null;
        } else {
            c.moveToFirst();
            trainingSession = Training.from(c);
        }

        c.close();
        return trainingSession;
    }

    public List<Training> getAllTrainings() {
        Cursor c = getCursor(null, null, KEY_START + " DESC");
        List<Training> trainingSessions = new ArrayList<>();
        if (c == null) {
            return trainingSessions;
        }
        while (c.moveToNext()) {
            trainingSessions.add(Training.from(c));
        }
        c.close();
        return trainingSessions;
    }

    protected int updateTraining(Training item) {
        ContentValues values = item.toContentValues();

        String selection = KEY_ID + " = ?";
        String[] selectionArgs = {String.valueOf(item.getId())};

        int rowsAffected = getDatabase(this).update(
                TABLE_NAME,
                values,
                selection,
                selectionArgs);
        return rowsAffected;
    }

    public void deleteTraining(Training item) {
        if (item == null || item.getId() <= 0) {
            return;
        }
        String selection = KEY_ID + " = ?";
        String[] selectionArgs = {String.valueOf(item.getId())};
        getDatabase(this).delete(TABLE_NAME, selection, selectionArgs);
    }

    public void deleteAllTrainings(){
        getDatabase(this).execSQL("delete from " + TABLE_NAME);
    }

    protected Cursor getCursor(String selection, String[] selectionArgs){
        return getCursor(selection, selectionArgs, KEY_ID + " ASC");
    }

    protected Cursor getCursor(String selection, String[] selectionArgs, String sortOrder) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                KEY_ID,
                KEY_NAME,
                KEY_DESCRIPTION,
                KEY_STEPS,
                KEY_DISTANCE,
                KEY_CALORIES,
                KEY_START,
                KEY_END,
                KEY_FEELING
        };

        return getDatabase(this).query(
                TABLE_NAME,  // The table to query
                projection,                                            // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
    }

    public static abstract class TrainingSessionEntry implements BaseColumns {
        public static final String TABLE_NAME = TrainingDbHelper.TABLE_NAME;
        public static final String KEY_NAME = TrainingDbHelper.KEY_NAME;
        public static final String KEY_DESCRIPTION = TrainingDbHelper.KEY_DESCRIPTION;
        public static final String KEY_STEPS = TrainingDbHelper.KEY_STEPS;
        public static final String KEY_CALORIES = TrainingDbHelper.KEY_CALORIES;
        public static final String KEY_DISTANCE = TrainingDbHelper.KEY_DISTANCE;
        public static final String KEY_START = TrainingDbHelper.KEY_START;
        public static final String KEY_END = TrainingDbHelper.KEY_END;
        public static final String KEY_FEELING = TrainingDbHelper.KEY_FEELING;
    }
}
