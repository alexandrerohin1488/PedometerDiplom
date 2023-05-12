package org.secuso.privacyfriendlyactivitytracker.persistence;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.secuso.privacyfriendlyactivitytracker.models.WalkingMode;

import java.util.List;


public class WalkingModePersistenceHelper {

    public static final String BROADCAST_ACTION_WALKING_MODE_CHANGED = "org.secuso.privacyfriendlystepcounter.WALKING_MODE_CHANGED";
    public static final String BROADCAST_EXTRA_OLD_WALKING_MODE = "org.secuso.privacyfriendlystepcounter.EXTRA_OLD_WALKING_MODE";
    public static final String BROADCAST_EXTRA_NEW_WALKING_MODE = "org.secuso.privacyfriendlystepcounter.EXTRA_NEW_WALKING_MODE";
    public static final String LOG_CLASS = WalkingModePersistenceHelper.class.getName();

    public static List<WalkingMode> getAllItems(Context context) {
        return new WalkingModeDbHelper(context).getAllWalkingModes();
    }

    public static WalkingMode getItem(long id, Context context) {
        return new WalkingModeDbHelper(context).getWalkingMode((int) id);
    }

    public static WalkingMode save(WalkingMode item, Context context) {
        if (item == null) {
            return null;
        }
        if (item.getId() <= 0) {
            long insertedId = insert(item, context);
            if (insertedId == -1) {
                return null;
            } else {
                item.setId(insertedId);
                return item;
            }
        } else {
            int affectedRows = update(item, context);
            if (affectedRows == 0) {
                return null;
            } else {
                return item;
            }
        }
    }

    public static boolean delete(WalkingMode item, Context context) {
        new WalkingModeDbHelper(context).deleteWalkingMode(item);
        return true;
    }

    public static boolean softDelete(WalkingMode item, Context context) {
        if (item == null || item.getId() <= 0) {
            return false;
        }
        item.setIsDeleted(true);
        return save(item, context).isDeleted();
    }

    public static boolean setActiveMode(WalkingMode mode, Context context) {
        Log.i(LOG_CLASS, "Changing active mode to " + mode.getName());
        if (mode.isActive()) {
            // Already active
            Log.i(LOG_CLASS, "Skipping active mode change");
            return true;
        }
        WalkingMode currentActiveMode = getActiveMode(context);

        if (currentActiveMode != null) {
            currentActiveMode.setIsActive(false);
            save(currentActiveMode, context);
        }
        mode.setIsActive(true);
        boolean success = save(mode, context).isActive();
        // broadcast the event
        Intent localIntent = new Intent(BROADCAST_ACTION_WALKING_MODE_CHANGED);
        localIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        if (currentActiveMode != null) {
            localIntent.putExtra(BROADCAST_EXTRA_OLD_WALKING_MODE, currentActiveMode.getId());
        }
        localIntent.putExtra(BROADCAST_EXTRA_NEW_WALKING_MODE, mode.getId());
        // Broadcasts the Intent to receivers in this app.
        context.sendBroadcast(localIntent);
        return success;
    }

    public static WalkingMode getActiveMode(Context context) {
        return new WalkingModeDbHelper(context).getActiveWalkingMode();
    }

    protected static long insert(WalkingMode item, Context context) {
        return new WalkingModeDbHelper(context).addWalkingMode(item);
    }

    protected static int update(WalkingMode item, Context context) {
        return new WalkingModeDbHelper(context).updateWalkingMode(item);
    }
}
