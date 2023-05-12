package org.secuso.privacyfriendlyactivitytracker.services;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

import androidx.annotation.NonNull;

public class HardwareStepService extends AbstractStepDetectorService {
    private float mStepOffset = -1;

    public HardwareStepService() {
        super();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_STEP_DETECTOR:
                this.onStepDetected(1);
                break;
            case Sensor.TYPE_STEP_COUNTER:
                if (this.mStepOffset < 0) {
                    this.mStepOffset = event.values[0];
                }
                if (this.mStepOffset > event.values[0]) {
                    // this should never happen?
                    return;
                }
                // publish difference between last known step count and the current ones.
                this.onStepDetected((int) (event.values[0] - mStepOffset));
                // Set offset to current value so we know it at next event
                mStepOffset = event.values[0];
                break;
        }
    }
}
