package me.demetoir.a3dsound_ndk;

import android.util.Log;
import android.widget.SeekBar;

import java.util.Random;

class soundOrbit extends Thread {
    private final static String TAG = "soundOrbit ";

    private final static int MODE_NONE = 0;
    private final static int MODE_CIRCLE = 1;
    private final static int MODE_LINE = 2;
    private final static int MODE_RANDOM = 3;

    private final static int FRAME_RATE = 25;
    private final static int UPDATE_TIME_INTERVAL = 1000 / FRAME_RATE;
    private final static int THREAD_WAKE_UP_TIME = 1000 / FRAME_RATE;
    private final static double DX_ANGLE = 1.15;
    private final static double EPSILON = 1e-4;
    private final static double DEFAULT_SPEED = 3.0;
    final static double MAX_SPEED = DEFAULT_SPEED * 3;
    final static double MIN_SPEED = 0;

    private final static int RANDOM_INTERVAL_MAX_CNT = FRAME_RATE * 3;
    private final static int RANDOM_MIN = -300;
    private final static int RANDOM_MAX = 300;

    private final static int SCREEN_LEFT = -360;
    private final static int SCREEN_RIGHT = 360;
    private final static int SCREEN_TOP = -500;
    private final static int SCREEN_BOTTOM = 500;

    final static int DIRECTION_FORWARD = 1;
    final static int DIRECTION_REVERSE = -1;

    private int mSOHandle;
    private SoundObjectView mSoundObjectView;
    private SoundEngine mSoundEngine;
    private boolean mIsRunning;
    private double mSpeed;
    private int mDirection;
    private int mRandomIntervalCnt;
    private int mInternalOrbitMode;
    private Random random;
    private boolean mIsReachLineEnd;

    private boolean mIsRandomizeSpeed;

    void setRandomizeSpeed(boolean flag){
        mIsRandomizeSpeed = flag;
    }

    boolean isRandomizeSpeed(){
        return mIsRandomizeSpeed;
    }

    soundOrbit(SoundEngine soundEngine, int SOHandle) {
        mSoundEngine = soundEngine;
        mSOHandle = SOHandle;
        mIsRunning = false;
        mSpeed = DEFAULT_SPEED;
        mDirection = 1;
        mRandomIntervalCnt = 0;
        mInternalOrbitMode = MODE_NONE;
        random = new Random();
        mIsReachLineEnd = false;
        mIsRandomizeSpeed = false;
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


//            Log.i(TAG, "run: doing");

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


    private void update() {
        int orbitMode = mSoundEngine.getOrbitMode(mSOHandle);
        switch (orbitMode) {
            case MODE_CIRCLE:
                updateCircleOrbit();
                break;
            case MODE_LINE:
                updateLineOrbit();
                break;
            case MODE_RANDOM:
                updateRandomOrbit();
                break;
            case MODE_NONE:
                break;
            default:
                break;
        }
    }

    private void updateCircleOrbit() {
        double radius = mSoundEngine.getSORadius(mSOHandle);
        double oldAngle = mSoundEngine.getSOCenterAngle(mSOHandle);
        Point2D centerP = new Point2D();
        mSoundEngine.getSOCenterPoint(mSOHandle, centerP);

        // TODO 선택기능...
        //등선이동
        double dTheta = (360.0 * mSpeed * DX_ANGLE) / (2 * Math.PI * radius);
        double angle = oldAngle + dTheta * mDirection;

        //등각 이동
        // double angle = oldAngle + mSpeed * DX_ANGLE * mDirection;

        double x = radius * Math.cos(Math.toRadians(angle)) + centerP.x;
        double y = radius * Math.sin(Math.toRadians(angle)) + centerP.y;

        Point2D sop = new Point2D();
        mSoundEngine.getSOPoint(mSOHandle, sop);

        Point2D p = new Point2D(x, y);
        mSoundEngine.setSOPoint(mSOHandle, p);
    }

    private void updateLineOrbit() {
        if (mIsReachLineEnd) {
            mIsReachLineEnd = false;
            Point2D newP = new Point2D();
            mSoundEngine.getSOStartPoint(mSOHandle, newP);
            mSoundEngine.setSOPoint(mSOHandle, newP);
            return;
        }

        Point2D startP = new Point2D();
        mSoundEngine.getSOStartPoint(mSOHandle, startP);

        Point2D endP = new Point2D();
        mSoundEngine.getSOEndPoint(mSOHandle, endP);

        Point2D oldP = new Point2D();
        mSoundEngine.getSOPoint(mSOHandle, oldP);

        double angle = Math.atan2(endP.y - startP.y, endP.x - startP.x);
//        Log.i(TAG, "update: angle = " + angle);
        double dx = mSpeed * Math.cos(angle);
        double dy = mSpeed * Math.sin(angle);
        Point2D newP = new Point2D(oldP.x + (float) dx, oldP.y + (float) dy);

        //if over end point

        if (isOverEndPoint(newP, endP, startP)) {
            mIsReachLineEnd = true;
        }

        mSoundEngine.setSOPoint(mSOHandle, newP);
    }

    private void updateRandomOrbit() {
        mRandomIntervalCnt -= 1;

        if (mInternalOrbitMode == MODE_CIRCLE) {
            updateCircleOrbit();
        } else if (mInternalOrbitMode == MODE_LINE) {
            updateLineOrbit();
        }

        // TODO 이부분 자연스럽게 처리되도록 수정
        if (mRandomIntervalCnt <= 0
                || isSOOutOfBoarder()
                || isReachLineEnd()) {
            mIsReachLineEnd = false;
            randomizeInternalOrbitMode();
            mRandomIntervalCnt = RANDOM_INTERVAL_MAX_CNT;

            if (mInternalOrbitMode == MODE_LINE) {
                if(mIsRandomizeSpeed) {
                    randomizeSpeed();
                }

                // set line start point to current point
                Point2D curP = new Point2D();
                mSoundEngine.getSOPoint(mSOHandle, curP);
                mSoundEngine.setSOStartPoint(mSOHandle, curP);

                // set random line endpoint
                Point2D randEndP = new Point2D();

                randEndP.randomize(SCREEN_LEFT, SCREEN_TOP, SCREEN_RIGHT, SCREEN_BOTTOM);
                mSoundEngine.setSOEndPoint(mSOHandle, randEndP);

//                printLogPoint2D(randEndP);
            } else if (mInternalOrbitMode == MODE_CIRCLE) {
                // set random speed and direction
                if(isRandomizeSpeed()) {
                    randomizeSpeed();
                }
                randomizeDirection();

                // set random circle center point
                Point2D randCenterP = new Point2D();
                randCenterP.randomize(SCREEN_LEFT, SCREEN_TOP, SCREEN_RIGHT, SCREEN_BOTTOM);
                mSoundEngine.setSOCenterPoint(mSOHandle, randCenterP);

//                printLogPoint2D(randCenterP);
            }
        }
    }

    private boolean isSOOutOfBoarder() {
        Point2D p = new Point2D();
        mSoundEngine.getSOPoint(mSOHandle, p);

//        printLogPoint2D(p);

        return !(p.x >= SCREEN_LEFT && p.x <= SCREEN_RIGHT
                && p.y >= SCREEN_TOP && p.y <= SCREEN_BOTTOM);
    }

    private boolean isReachLineEnd() {
        return mInternalOrbitMode == MODE_LINE && mIsReachLineEnd;
    }

    private void randomizeSpeed() {
        //randomize speed
        mSpeed = (random.nextFloat() * MAX_SPEED) + MIN_SPEED;

        //update seekBar progress
        MainActivity activity = (MainActivity) mSoundEngine.getActivity();
        SeekBar seekBar = (SeekBar) activity.findViewById(R.id.seekBarSoundObjectSpeed);
        int max = seekBar.getMax();
        int progress = (int) ((mSpeed - MIN_SPEED) * max / MAX_SPEED);
        seekBar.setProgress(progress);
    }

    private void randomizeInternalOrbitMode() {
        mInternalOrbitMode = random.nextInt(1 + 1) + 1;
    }

    private void randomizeDirection() {
        if (random.nextInt(1 + 1) == 0) {
            mDirection = 1;
        } else {
            mDirection = -1;
        }
    }

    void startRunning() {
        mIsRunning = true;
    }

    void stopRunning() {
        mIsRunning = false;
    }

    private boolean isOverEndPoint(Point2D curP, Point2D endP, Point2D startP) {
        double a = (endP.y - startP.y) / (endP.x - startP.x) + EPSILON;
        double ia = 1 / a;
        double b = endP.y - endP.x * ia;

//        Log.i(TAG, "isOverEndPoint: a " + a + " b" + b);
        return (curP.x * ia - curP.y + b) * (startP.x * ia - startP.y + b) < 0;
    }

    int getInternalOrbitMode() {
        return mInternalOrbitMode;
    }

    private void printLogPoint2D(Point2D p) {
        Log.i(TAG, "printLogPoint2D: x " + p.x + " y " + p.y);
    }


    public int getDirection() {
        return mDirection;
    }

    public void setDirection(int direction) {
        mDirection = direction;
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
    }

    public float getSpeed() {
        return (float) mSpeed;
    }

}
