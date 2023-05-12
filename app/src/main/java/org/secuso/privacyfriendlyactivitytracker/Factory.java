package org.secuso.privacyfriendlyactivitytracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import org.secuso.privacyfriendlyactivitytracker.services.AbstractStepDetectorService;
import org.secuso.privacyfriendlyactivitytracker.services.AccelerometerStepDetectorService;
import org.secuso.privacyfriendlyactivitytracker.services.HardwareStepService;
import org.secuso.privacyfriendlyactivitytracker.utils.AndroidVersionHelper;

public class Factory {

    public static Class<? extends AbstractStepDetectorService> getStepDetectorServiceClass(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        PackageManager pm = context.getPackageManager();
        if(pm != null && AndroidVersionHelper.supportsStepDetector(pm) && sharedPref.getBoolean(context.getString(R.string.pref_use_step_hardware), false)) {
            return HardwareStepService.class;
        }else{
            return AccelerometerStepDetectorService.class;
        }
    }
}
