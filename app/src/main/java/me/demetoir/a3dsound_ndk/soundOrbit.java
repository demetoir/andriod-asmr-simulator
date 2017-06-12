package me.demetoir.a3dsound_ndk;

import android.util.Log;

class soundOrbit extends Thread {
    private final static String TAG = "soundOrbit ";

    private static final int FRAME_RATE = 25;
    private static final int UPDATE_TIME_INTERVAL = 100;
    private final static int THREAD_WAKE_UP_TIME = 1000 / FRAME_RATE;
    private final static double dx_angle = 1;

    private int mSOHandle;
    private SoundObjectView mSoundObjectView;
    private SoundEngine mSoundEngine;
    private boolean mIsRunning;

    soundOrbit(SoundEngine soundEngine, int SOHandle) {
        mSoundEngine = soundEngine;
        mSOHandle = SOHandle;
        mIsRunning = false;
    }

    soundOrbit(SoundEngine soundEngine,
               int SOHandle,
               SoundObjectView soundObjectView) {
        mSoundEngine = soundEngine;
        mSOHandle = SOHandle;
        mSoundObjectView = soundObjectView;
        mIsRunning = false;
    }

    void setOrbitView(SoundObjectView soundObjectView) {
        mSoundObjectView = soundObjectView;
    }


    @Override
    public void run() {
        while (true) {
            if (!mIsRunning) {
                try {
                    sleep(THREAD_WAKE_UP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }


            Log.i(TAG, "run: doing");
            if (!mSoundObjectView.getIsTouching()) {
                update();
                mSoundObjectView.post(new Runnable() {
                    @Override
                    public void run() {
                        mSoundObjectView.update();
                    }
                });

                try {
                    sleep(UPDATE_TIME_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void update() {
        double distance = mSoundEngine.getSODistance(mSOHandle);
        double oldAngle = mSoundEngine.getSOAngle(mSOHandle);
        Log.i(TAG, "update: old angle " + oldAngle);
        double angle;

        angle = oldAngle + dx_angle;
        Log.i(TAG, "update: angle " + dx_angle);
        double x = distance * Math.cos(Math.toRadians(angle));
        double y = distance * Math.sin(Math.toRadians(angle));
        mSoundEngine.setSOX(mSOHandle, (float) x);
        mSoundEngine.setSOY(mSOHandle, (float) y);

        Log.i(TAG, "update: set " + x + "  " + y);

    }

    public void startRunning() {
        mIsRunning = true;
    }

    public void stopRunning() {
        mIsRunning = false;
    }
}
