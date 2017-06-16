package me.demetoir.a3dsound_ndk;

import android.util.Log;

class soundOrbit extends Thread {
    private final static String TAG = "soundOrbit ";

    public final static int MODE_NONE = 0;
    public final static int MODE_CIRCLE = 1;
    public final static int MODE_LINE = 2;
    public final static int MODE_RANDOM = 3;

    private final static int FRAME_RATE = 25;
    private final static int UPDATE_TIME_INTERVAL = 1000 / FRAME_RATE;

    private final static int THREAD_WAKE_UP_TIME = 1000 / FRAME_RATE;
    private final static double DX_ANGLE = 1.15;
    private final static double EPSILON = 1e-4;
    private final static double default_speed = 3.0;

    private int mSOHandle;
    private SoundObjectView mSoundObjectView;
    private SoundEngine mSoundEngine;
    private boolean mIsRunning;
    private double mSpeed;
    private int mDirection;

    soundOrbit(SoundEngine soundEngine, int SOHandle) {
        mSoundEngine = soundEngine;
        mSOHandle = SOHandle;
        mIsRunning = false;
        mSpeed = default_speed;
        mDirection = 1;
    }

    soundOrbit(SoundEngine soundEngine,
               int SOHandle,
               SoundObjectView soundObjectView) {
        mSoundEngine = soundEngine;
        mSOHandle = SOHandle;
        mIsRunning = false;
        mSpeed = default_speed;
        mSoundObjectView = soundObjectView;
        mDirection = 1;
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


    public void update() {
        if (mSoundEngine.getOrbitMode(mSOHandle) == MODE_CIRCLE) {
            double radius = mSoundEngine.getSORadius(mSOHandle);
            double oldAngle = mSoundEngine.getSOCenterAngle(mSOHandle);
            Point2D centerP = new Point2D();
            mSoundEngine.getSOCenterPoint(mSOHandle, centerP);

            // TODO 선택기능...
            //등선이동
            double dTheta = (360.0 * mSpeed * DX_ANGLE) / (2 * Math.PI * radius);
            double angle = oldAngle + dTheta * mDirection;

            //등각 이동
            // double angle = oldAngle + mSpeed * DX_ANGLE;

            double x = radius * Math.cos(Math.toRadians(angle)) + centerP.x;
            double y = radius * Math.sin(Math.toRadians(angle)) + centerP.y;

            Point2D sop = new Point2D();
            mSoundEngine.getSOPoint(mSOHandle, sop);

            Point2D p = new Point2D(x, y);
            mSoundEngine.setSOPoint(mSOHandle, p);
        } else if (mSoundEngine.getOrbitMode(mSOHandle) == MODE_LINE) {
            Point2D startP = new Point2D();
            mSoundEngine.getSOStartPoint(mSOHandle, startP);

            Point2D endP = new Point2D();
            mSoundEngine.getSOEndPoint(mSOHandle, endP);

            Point2D oldP = new Point2D();
            mSoundEngine.getSOPoint(mSOHandle, oldP);

            double angle = Math.atan2(endP.y - startP.y, endP.x - startP.x);
            Log.i(TAG, "update: angle = " + angle);
            double dx = mSpeed * Math.cos(angle);
            double dy = mSpeed * Math.sin(angle);
            Point2D newP = new Point2D(oldP.x + (float) dx, oldP.y + (float) dy);

            //if over end point
            if (isOverEndPoint(newP, endP, startP)) {
                newP = startP;
            }

            mSoundEngine.setSOPoint(mSOHandle, newP);
        }
    }

    public void startRunning() {
        mIsRunning = true;
    }

    public void stopRunning() {
        mIsRunning = false;
    }

    public boolean isOverEndPoint(Point2D curP, Point2D endP, Point2D startP) {
        double a = (endP.y - startP.y) / (endP.x - startP.x) + EPSILON;
        double ia = 1 / a;
        double b = endP.y - endP.x * ia;

        Log.i(TAG, "isOverEndPoint: a " + a + " b" + b);
        return (curP.x * ia - curP.y + b) * (startP.x * ia - startP.y + b) < 0;
    }


}
