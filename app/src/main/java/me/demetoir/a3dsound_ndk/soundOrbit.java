package me.demetoir.a3dsound_ndk;

import android.util.Log;
import android.widget.ImageView;

import static android.content.ContentValues.TAG;

/**
 * Created by Yujun-desktop on 2017-05-31.
 */

class soundOrbit extends Thread {

    private static final int UPDATE_TIME_INTERVAL = 10;
    private final static int THREAD_WAKE_UP_TIME = 100;
    private final static double dx_angle = 3;

    private int mSOHandle;
    private ImageView mSoundSourceImageView;
    private OrbitView mOrbitView;
    private SoundEngine mSoundEngine;
    private boolean mIsRunning;

    soundOrbit(SoundEngine soundEngine, int SOHandle) {
        mSoundEngine = soundEngine;
        mSOHandle = SOHandle;
        mIsRunning = false;
    }

    soundOrbit(SoundEngine soundEngine, int SOHandle,
               ImageView soundSourceImageView, OrbitView orbitView) {
        mSoundEngine = soundEngine;
        mSOHandle = SOHandle;
        mSoundSourceImageView = soundSourceImageView;
        mOrbitView = orbitView;
        mIsRunning = false;
    }

    void setOrbitView(OrbitView orbitView) {
        mOrbitView = orbitView;
    }

    void setmSoundSourceImageView(ImageView soundSourceImageView) {
        mSoundSourceImageView = soundSourceImageView;
    }


    @Override
    public void run() {
        super.run();


    }

    private void task() {
        while (true) {
            if (!mIsRunning) {
                try {
                    sleep(THREAD_WAKE_UP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (!mOrbitView.getIsTouching())
                update();
        }
    }

    public void update() {
        double distance = mSoundEngine.getSODistance(mSOHandle);
        double oldAngle = mSoundEngine.getSOAngle(mSOHandle);

        double angle;
        if (oldAngle < 0) {
            angle = (180 - oldAngle) + 180;
        } else {
            angle = oldAngle;
        }

        double x = distance * Math.cos(angle);
        double y = distance * Math.sin(angle);
        mSoundEngine.setSOX(mSOHandle, (float) x);
        mSoundEngine.setSOY(mSOHandle, (float) y);

        Log.i(TAG, "update: set x,y");

        mOrbitView.invalidate();
        mSoundSourceImageView.invalidate();
        Log.i(TAG, "update: invalidate");

    }

    public void startRunning() {
        mIsRunning = true;
    }

    public void stopRunning() {
        mIsRunning = false;
    }
}
