
package org.secuso.privacyfriendlyactivitytracker.activities;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;

import org.secuso.privacyfriendlyactivitytracker.R;
import org.secuso.privacyfriendlyactivitytracker.models.StepCount;
import org.secuso.privacyfriendlyactivitytracker.persistence.StepCountPersistenceHelper;
import org.secuso.privacyfriendlyactivitytracker.receivers.StepCountPersistenceReceiver;
import org.secuso.privacyfriendlyactivitytracker.utils.AndroidVersionHelper;
import org.secuso.privacyfriendlyactivitytracker.utils.StepDetectionServiceHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreferencesActivity extends AppCompatPreferenceActivity {
    private static Map<String, String> additionalSummaryTexts;
    static int REQUEST_EXTERNAL_STORAGE = 2;
    static int REQUEST_LOCATION = 1;
    static int REQUEST_ACTIVITY = 3;

    private GeneralPreferenceFragment generalPreferenceFragment;

    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                String additionalSummaryText = additionalSummaryTexts.get(preference.getKey());
                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? (((additionalSummaryText != null) ? additionalSummaryText : "") + listPreference.getEntries()[index])
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    public PreferencesActivity() {
        super();
        additionalSummaryTexts = new HashMap<>();
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private static void bindPreferenceSummaryToLongValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getLong(preference.getKey(), 0));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
                //|| HelpFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportCSVafterPermissionGranted();
            } else {
                Toast.makeText(this, getString(R.string.export_csv_permission_needed), Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_LOCATION) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) || permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        // location permission was not granted - disable velocity setting.
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean(getString(R.string.pref_show_velocity), false);
                        editor.apply();
                    }
                }
            }
        }
        if (requestCode == REQUEST_ACTIVITY){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (generalPreferenceFragment !=null){
                    generalPreferenceFragment.saveStepsAndRestartService();
                    generalPreferenceFragment.checkHardwareStepUse(true);
                }
            } else {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.pref_use_step_hardware), false);
                editor.apply();
                if (generalPreferenceFragment !=null){
                    generalPreferenceFragment.checkHardwareStepUse(false);
                }
            }
        }

    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private Preference exportDataPreference;
        private Preference lengthUnitPreference;
        private Preference energyUnitPreference;
        private Preference dailyStepGoalPreference;
        private Preference weightPreference;
        private Preference genderPreference;
        private Preference accelThresholdPreference;
        private Preference useStepHardwarePreference;
        private Preference stepCounterEnabledPreference;
        private static String LOG_TAG = GeneralPreferenceFragment.class.getName();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            ((PreferencesActivity) getActivity()).generalPreferenceFragment = this;

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            additionalSummaryTexts.put(getString(R.string.pref_accelerometer_threshold), getString(R.string.pref_summary_accelerometer_threshold));

            lengthUnitPreference = findPreference(getString(R.string.pref_unit_of_length));
            stepCounterEnabledPreference = findPreference(getString(R.string.pref_step_counter_enabled));
            energyUnitPreference = findPreference(getString(R.string.pref_unit_of_energy));
            dailyStepGoalPreference = findPreference(getString(R.string.pref_daily_step_goal));
            weightPreference = findPreference(getString(R.string.pref_weight));
            genderPreference = findPreference(getString(R.string.pref_gender));
            accelThresholdPreference = findPreference(getString(R.string.pref_accelerometer_threshold));
            exportDataPreference = findPreference(getString(R.string.pref_export_data));
            useStepHardwarePreference = findPreference(getString(R.string.pref_use_step_hardware));

            bindPreferenceSummaryToValue(lengthUnitPreference);
            bindPreferenceSummaryToValue(energyUnitPreference);
            bindPreferenceSummaryToValue(dailyStepGoalPreference);
            bindPreferenceSummaryToValue(weightPreference);
            bindPreferenceSummaryToValue(genderPreference);
            bindPreferenceSummaryToValue(accelThresholdPreference);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            sharedPref.registerOnSharedPreferenceChangeListener(this);

            //correctly set background counting enabled or not
            findPreference(getString(R.string.pref_hw_background_counter_frequency)).setEnabled(sharedPref.getBoolean(getString(R.string.pref_use_step_hardware), true));
            findPreference(getString(R.string.pref_which_step_hardware)).setEnabled(sharedPref.getBoolean(getString(R.string.pref_use_step_hardware), true));


            stepCounterEnabledPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    StepCountPersistenceReceiver.unregisterSaveListener();
                    return true;
                }
            });

            useStepHardwarePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SwitchPreference pref = (SwitchPreference) preference;
                    Boolean enable = pref.isChecked();

                    saveStepsAndRestartService();

                    if(enable) {

                        if (AndroidVersionHelper.supportsStepDetector(getActivity().getApplicationContext().getPackageManager())) {

                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                if(verifyActivityPermissions(getActivity())) {
                                    checkHardwareStepUse(true);
                                }
                            }
                            return true;

                        } else {
                            Toast.makeText(getActivity(), R.string.pref_use_step_hardware_not_available, Toast.LENGTH_SHORT).show();
                            checkHardwareStepUse(false);
                            return false;
                        }
                    }
                    checkHardwareStepUse(false);
                    return false;
                }
            });

            exportDataPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //Check if you have the permission
                    if (verifyStoragePermissions(getActivity())) {
                        generateCSVToExport();
                    }
                    return true;
                }
            });
        }

        private void checkHardwareStepUse(Boolean checked) {
            final SwitchPreference hardwarePreference = (SwitchPreference) findPreference(getString(R.string.pref_use_step_hardware));
            if (hardwarePreference!=null) hardwarePreference.setChecked(checked);
        }

        private void saveStepsAndRestartService() {
            final Context context = getActivity().getApplicationContext();
/*
            StepCountPersistenceReceiver.registerSaveListener(new StepCountPersistenceReceiver.ISaveListener() {
                @Override
                public void onSaveDone() {
                    //Log.d("save", "save done");
                    StepCountPersistenceReceiver.unregisterSaveListener();

                    if(context != null) {
                        StepDetectionServiceHelper.startAllIfEnabled(true, context);
                    }
                }
            });

            StepDetectionServiceHelper.cancelPersistenceService(true, context);
            StepDetectionServiceHelper.stopAllIfNotRequired(false, context);*/
            StepDetectionServiceHelper.restartStepDetection(context);
        }

        private boolean verifyStoragePermissions(Activity activity) {
            String[] PERMISSIONS_STORAGE = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            // Check if we have write permission
            int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
                return false;
            }
            return true;
        }

        private boolean verifyActivityPermissions(Activity activity) {
            String[] PERMISSION_ACTIVITY = {
                    Manifest.permission.ACTIVITY_RECOGNITION
            };

            // Check if we have permission
            int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACTIVITY_RECOGNITION);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSION_ACTIVITY,
                        REQUEST_ACTIVITY
                );
                return false;
            }
            return true;
        }

        public File generateCSVToExport() {
            final Context context = getActivity().getApplicationContext();
            SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String csvFileName = "exportStepCount_" + fileDateFormat.format(System.currentTimeMillis()) + ".csv";

            //Get List of StepCounts
            List<StepCount> steps = StepCountPersistenceHelper.getStepCountsForever(getActivity());
            try {
                File csvFile = new File(Environment.getExternalStoragePublicDirectory(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT ?
                        Environment.DIRECTORY_DOCUMENTS : context.getResources().getString(R.string.app_name)), csvFileName);
                csvFile.getParentFile().mkdirs();
                Log.i(LOG_TAG, "Exporting steps to: " + csvFile.getAbsolutePath());
                //Generate the file
                OutputStreamWriter csvWriter = new OutputStreamWriter(new FileOutputStream(csvFile));
                //Add the header
                csvWriter.write(getString(R.string.export_csv_header) + "\r\n");
                //Populate the file
                String dateFormat = "yyyy-MM-dd HH:mm:ss";
                for(StepCount s : steps)
                {
                    String startDate = s.getStartTime() == 0 ? getString(R.string.export_csv_begin) : DateFormat.format(dateFormat, new Date(s.getStartTime())).toString();
                    String endDate = DateFormat.format(dateFormat, new Date(s.getEndTime())).toString();
                    csvWriter.write(startDate + ";" + endDate + ";" + s.getStepCount() + ";" + s.getWalkingMode().getName() + "\r\n");
                }
                csvWriter.close();
                //Display message
                Toast.makeText(getActivity(), getString(R.string.export_csv_success) + " " + csvFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                return csvFile;
            } catch (IOException e) {
                Toast.makeText(getActivity(), getString(R.string.export_csv_error), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                this.getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onDetach() {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            sharedPref.unregisterOnSharedPreferenceChangeListener(this);
            super.onDetach();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Context context = getActivity().getApplicationContext();
            Log.d("preference check","pref changed: "+key);


            // Detect changes on preferences and update our internal variable
            if (key.equals(getString(R.string.pref_step_counter_enabled))) {
                boolean isEnabled = sharedPreferences.getBoolean(getString(R.string.pref_step_counter_enabled), true);
                if (isEnabled) {
                    StepDetectionServiceHelper.startAllIfEnabled(context);
                } else {
                    StepDetectionServiceHelper.stopAllIfNotRequired(context);
                }
            }

            // check for location permission
            if (key.equals(getString(R.string.pref_show_velocity))) {
                boolean isEnabled = sharedPreferences.getBoolean(getString(R.string.pref_show_velocity), false);
                if (isEnabled) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                    }
                } else {
                    final SwitchPreference velocityPreference = (SwitchPreference) findPreference(getString(R.string.pref_show_velocity));
                    if (velocityPreference!=null) velocityPreference.setChecked(false);
                }
            }

            if (key.equals(getString(R.string.pref_use_step_hardware))) {
                boolean isEnabled = sharedPreferences.getBoolean(getString(R.string.pref_use_step_hardware), true);
                findPreference(getString(R.string.pref_hw_background_counter_frequency)).setEnabled(isEnabled);
                findPreference(getString(R.string.pref_which_step_hardware)).setEnabled(isEnabled);
            }

            if (key.equals(getString(R.string.pref_hw_background_counter_frequency)) && sharedPreferences.getString(getString(R.string.pref_which_step_hardware), "0").equals("0")) {
                saveStepsAndRestartService();
            }
            if (key.equals(getString(R.string.pref_which_step_hardware))) {
                saveStepsAndRestartService();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToLongValue(findPreference(getString(R.string.pref_notification_motivation_alert_time)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notification_motivation_alert_criterion)));

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            sharedPref.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                this.getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onDetach() {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            sharedPref.unregisterOnSharedPreferenceChangeListener(this);
            super.onDetach();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (isDetached()) {
                return;
            }
            // Detect changes on preferences and update our internal variable
            if (key.equals(getString(R.string.pref_notification_motivation_alert_enabled)) || key.equals(getString(R.string.pref_notification_motivation_alert_time))) {
                boolean isEnabled = sharedPreferences.getBoolean(getString(R.string.pref_notification_motivation_alert_enabled), true);
                if (isEnabled) {
                    StepDetectionServiceHelper.startAllIfEnabled(getActivity().getApplicationContext());
                } else {
                    StepDetectionServiceHelper.stopAllIfNotRequired(getActivity().getApplicationContext());
                }
            }
        }
    }


    void exportCSVafterPermissionGranted() {
        generalPreferenceFragment.generateCSVToExport();
    }

//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static class HelpFragment extends PreferenceFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.help);
//            setHasOptionsMenu(false);
//        }
//
//        @Override
//        public boolean onOptionsItemSelected(MenuItem item) {
//            int id = item.getItemId();
//            if (id == android.R.id.home) {
//                this.getActivity().onBackPressed();
//                return true;
//            }
//            return super.onOptionsItemSelected(item);
//        }
//    }
}
