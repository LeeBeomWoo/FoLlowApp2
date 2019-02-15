package com.example.followapp.cameraapi;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;
import android.view.WindowManager;

public class rotationListenerHelper {
    private int lastRotation;

    private WindowManager windowManager;
    private OrientationEventListener orientationEventListener;

    private rotationCallbackFn callback;

    public rotationListenerHelper() {
    }

    public void listen(Context context, rotationCallbackFn callback) {
        // registering the listening only once.
        stop();
        context = context.getApplicationContext();
        this.callback = callback;
        this.windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);

        this.orientationEventListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                WindowManager localWindowManager = windowManager;
                rotationCallbackFn localCallback = rotationListenerHelper.this.callback;
                if(windowManager != null && localCallback != null) {
                    int newRotation = localWindowManager.getDefaultDisplay().getRotation();
                    if (newRotation != lastRotation) {
                        localCallback.onRotationChanged(lastRotation, newRotation);
                        lastRotation = newRotation;
                    }
                }
            }
        };
        this.orientationEventListener.enable();

        lastRotation = windowManager.getDefaultDisplay().getRotation();
    }

    public void stop() {
        if(this.orientationEventListener != null) {
            this.orientationEventListener.disable();
        }
        this.orientationEventListener = null;
        this.windowManager = null;
        this.callback = null;
    }
    public interface rotationCallbackFn {
        void onRotationChanged(int lastRotation, int newRotation);
    }
}
