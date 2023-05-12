
package org.secuso.privacyfriendlyactivitytracker.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.secuso.privacyfriendlyactivitytracker.R;
import org.secuso.privacyfriendlyactivitytracker.fragments.DailyReportFragment;
import org.secuso.privacyfriendlyactivitytracker.fragments.MainFragment;
import org.secuso.privacyfriendlyactivitytracker.fragments.MonthlyReportFragment;
import org.secuso.privacyfriendlyactivitytracker.fragments.WeeklyReportFragment;
import org.secuso.privacyfriendlyactivitytracker.utils.StepDetectionServiceHelper;


public class MainActivity extends BaseActivity implements DailyReportFragment.OnFragmentInteractionListener, WeeklyReportFragment.OnFragmentInteractionListener, MonthlyReportFragment.OnFragmentInteractionListener {

    private static final int ACTIVITY_RECOGNITION = 1;
    private boolean askedAlready = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init preferences
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

        // Load first view
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, new MainFragment(), "MainFragment");
        fragmentTransaction.commit();

        // Start step detection if enabled and not yet started
        StepDetectionServiceHelper.startAllIfEnabled(this);
        //Log.i(LOG_TAG, "MainActivity initialized");
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.menu_home;
    }

    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACTIVITY_RECOGNITION }, ACTIVITY_RECOGNITION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // if permission is not granted ask again
        if(requestCode == ACTIVITY_RECOGNITION) {
            if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                if(askedAlready) {
                    builder.setMessage(R.string.dialog_permission_activity_recognition_2);
                    builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    });
                } else {
                    builder.setMessage(R.string.dialog_permission_activity_recognition);
                    builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        requestPermission();
                        dialogInterface.dismiss();
                    });
                }
                builder.create().show();
            }
        }
    }
}
