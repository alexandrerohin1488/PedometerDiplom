package org.secuso.privacyfriendlyactivitytracker.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import org.secuso.privacyfriendlyactivitytracker.R;
import org.secuso.privacyfriendlyactivitytracker.models.WalkingMode;

import java.util.ArrayList;
import java.util.List;

public class WalkingModeDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "WalkingModes.db";

    public static final String TABLE_NAME = "walkingmodes";

    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_STEP_SIZE = "stepsize";
    public static final String KEY_STEP_FREQUENCY = "stepfrequency";
    public static final String KEY_IS_ACTIVE = "is_active";
    public static final String KEY_IS_DELETED = "deleted";

    private static final String INTEGER_TYPE = " INTEGER";
    private static final String STRING_TYPE = " TEXT";
    private static final String REAL_TYPE = " REAL";

    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + WalkingModeEntry.TABLE_NAME + " (" +
                    WalkingModeEntry._ID + " INTEGER PRIMARY KEY," +
                    WalkingModeEntry.KEY_NAME + STRING_TYPE + COMMA_SEP +
                    WalkingModeEntry.KEY_STEP_SIZE + REAL_TYPE + COMMA_SEP +
                    WalkingModeEntry.KEY_STEP_FREQUENCY + REAL_TYPE + COMMA_SEP +
                    WalkingModeEntry.KEY_IS_ACTIVE + INTEGER_TYPE + COMMA_SEP +
                    WalkingModeEntry.KEY_IS_DELETED + INTEGER_TYPE +
                    " )";
    private static final String LOG_CLASS = WalkingModeDbHelper.class.getName();
    private static SQLiteDatabase db;

    private Context context;

    private static SQLiteDatabase getDatabase(WalkingModeDbHelper instance){
        if(db == null){
            db = instance.getWritableDatabase();
        }
        return db;
    }

    public static void invalidateReference(){
        db = null;
    }

    public WalkingModeDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }


    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        // Insert default walking modes
        String[] walkingModesNames = context.getResources().getStringArray(R.array.pref_default_walking_mode_names);
        String[] walkingModesStepLengthStrings = context.getResources().getStringArray(R.array.pref_default_walking_mode_step_lenghts);
        if (walkingModesStepLengthStrings.length != walkingModesNames.length) {
            Log.e(LOG_CLASS, "Number of default walking mode step lengths and names have to be the same.");
            return;
        }
        if (walkingModesNames.length == 0) {
            Log.e(LOG_CLASS, "There are no default walking modes.");
        }
        for (int i = 0; i < walkingModesStepLengthStrings.length; i++) {
            String stepLengthString = walkingModesStepLengthStrings[i];
            double stepLength = Double.valueOf(stepLengthString);
            String name = walkingModesNames[i];
            WalkingMode walkingMode = new WalkingMode();
            walkingMode.setStepLength(stepLength);
            walkingMode.setName(name);
            walkingMode.setIsActive(i == 0);
            this.addWalkingMode(walkingMode, db);
            Log.i(LOG_CLASS, "Created default walking mode " + name);
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Fill when upgrading DB
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public long addWalkingMode(WalkingMode walkingMode){
        return addWalkingMode(walkingMode, getDatabase(this));
    }

    public long addWalkingMode(WalkingMode walkingMode, SQLiteDatabase db){
        ContentValues values = walkingMode.toContentValues();
        return db.insert(
                TABLE_NAME,
                null,
                values);
    }

    public void addWalkingModeWithID(WalkingMode walkingMode){
        ContentValues values = walkingMode.toContentValues();
        values.put(KEY_ID, walkingMode.getId());
        getDatabase(this).insert(
                TABLE_NAME,
                null,
                values);
    }

    public WalkingMode getWalkingMode(int id){
        Cursor c = getCursor(KEY_ID + " = ?", new String[]{String.valueOf(id)});
        WalkingMode walkingMode;
        if (c == null) {
            return null;
        }
        if (c.getCount() == 0) {
            walkingMode = null;
        } else {
            c.moveToFirst();
            walkingMode = WalkingMode.from(c);
        }

        c.close();
        return walkingMode;
    }

    public WalkingMode getActiveWalkingMode() {
        Cursor c = getCursor(KEY_IS_ACTIVE + " = ?", new String[]{String.valueOf(true)});
        WalkingMode walkingMode;
        if (c.getCount() == 0) {
            walkingMode = null;
        } else {
            c.moveToFirst();
            walkingMode = WalkingMode.from(c);
        }
        c.close();
        return walkingMode;
    }

    public List<WalkingMode> getAllWalkingModes(){
        return this.getAllWalkingModes(false);
    }

    public List<WalkingMode> getAllWalkingModes(boolean withDeleted){
        Cursor c = getCursor(KEY_IS_DELETED + " = ?", new String[]{String.valueOf(withDeleted)});
        List<WalkingMode> walkingModes = new ArrayList<>();
        if (c == null) {
            return walkingModes;
        }
        while (c.moveToNext()) {
            walkingModes.add(WalkingMode.from(c));
        }
        c.close();
        return walkingModes;
    }

    public int updateWalkingMode(WalkingMode walkingMode){
        ContentValues values = walkingMode.toContentValues();

        String selection = KEY_ID + " = ?";
        String[] selectionArgs = {String.valueOf(walkingMode.getId())};

        return getDatabase(this).update(
                TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    public void deleteWalkingMode(WalkingMode walkingMode){
        if (walkingMode == null || walkingMode.getId() <= 0) {
            return;
        }
        String selection = KEY_ID + " = ?";
        String[] selectionArgs = {String.valueOf(walkingMode.getId())};
        getDatabase(this).delete(TABLE_NAME, selection, selectionArgs);
    }

    public void deleteAllWalkingModes(){
        getDatabase(this).execSQL("delete from " + TABLE_NAME);
    }

    private Cursor getCursor(String selection, String[] selectionArgs) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                KEY_ID,
                KEY_NAME,
                KEY_STEP_SIZE,
                KEY_STEP_FREQUENCY,
                KEY_IS_ACTIVE,
                KEY_IS_DELETED
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                KEY_ID + " ASC";

        return getDatabase(this).query(
                TABLE_NAME,                         // The table to query
                projection,                         // The columns to return
                selection,                          // The columns for the WHERE clause
                selectionArgs,                      // The values for the WHERE clause
                null,                               // don't group the rows
                null,                               // don't filter by row groups
                sortOrder                           // The sort order
        );
    }

    public static abstract class WalkingModeEntry implements BaseColumns {
        public static final String TABLE_NAME = WalkingModeDbHelper.TABLE_NAME;
        public static final String KEY_NAME = WalkingModeDbHelper.KEY_NAME;
        public static final String KEY_STEP_SIZE = WalkingModeDbHelper.KEY_STEP_SIZE;
        public static final String KEY_STEP_FREQUENCY = WalkingModeDbHelper.KEY_STEP_FREQUENCY;
        public static final String KEY_IS_ACTIVE = WalkingModeDbHelper.KEY_IS_ACTIVE;
        public static final String KEY_IS_DELETED = WalkingModeDbHelper.KEY_IS_DELETED;
    }
}
