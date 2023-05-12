package org.secuso.privacyfriendlyactivitytracker;

import android.app.Activity;
import android.app.Application;
import android.app.backup.BackupManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;


import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public class PFAPedometerApplication extends Application implements Configuration.Provider {


    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setMinimumLoggingLevel(Log.INFO).build();
    }

    private final AtomicBoolean lock = new AtomicBoolean(false);

    public void lock() {
        lock.set(true);
        showAlertDialog(shownView.get());
    }

    public void release() {
        lock.set(false);
    }

    private @NonNull
    WeakReference<Activity> shownView = new WeakReference<>(null);

    public void register(@NonNull Activity obs) {
        shownView = new WeakReference<>(obs);
        if (lock.get()) {
            showAlertDialog(obs);
        }
    }

    public void unregister() {
        shownView = new WeakReference<>(null);
    }

    private void showAlertDialog(Context context) {
        //AlertDialog.Builder builder = new AlertDialog.Builder(context);

    }
}
