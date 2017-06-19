package me.demetoir.a3dsound_ndk.SoundEngine;

import android.app.Activity;
import android.media.AudioTrack;
import android.util.Log;

import me.demetoir.a3dsound_ndk.MainActivity;
import me.demetoir.a3dsound_ndk.util.Point2D;


public class SoundEngine {
    private final static String TAG = "SoundEngine";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private final static int MAX_SPOHANDLE_SIZE = 10;
    private final static int DEFAULT_SO_HANDLE = 0;

    public final static int MODE_NONE = 0;
    public final static int MODE_CIRCLE = 1;
    public final static int MODE_LINE = 2;
    public final static int MODE_RANDOM = 3;

    private SoundProvider mProvider;
    private SoundConsumer mConsumer;
    private SoundBuffer mSoundBuffer;
    private AudioTrack mAudioTrack;
    private SoundOrbit mSoundOrbit;

    private MainActivity mainActivity;

    private int[] SPOHandleList;
    private boolean mIsPlaying;

    public SoundEngine(AudioTrack audioTrack) {
        mAudioTrack = audioTrack;
        mSoundBuffer = new SoundBuffer();

        SPOHandleList = new int[MAX_SPOHANDLE_SIZE];

        mConsumer = new SoundConsumer(mSoundBuffer, mAudioTrack);
        mProvider = new SoundProvider(mSoundBuffer, DEFAULT_SO_HANDLE);
        mIsPlaying = false;

        mConsumer.addSoundProvider(mProvider);
        mSoundOrbit = new SoundOrbit(this, DEFAULT_SO_HANDLE);
        mSoundOrbit.start();
    }

    public void setMainActivity(Activity activity) {
        mainActivity = (MainActivity) activity;
    }

    Activity getActivity() {
        return mainActivity;
    }

    public void loadHRTF_database(float[][] rightHRTF_database,
                                  float[][] leftHRTF_database,
                                  int angleIndexSize) {
        for (int angle = 0; angle < angleIndexSize; angle++) {
            loadHRTF(rightHRTF_database[angle], angle, 0);
            loadHRTF(leftHRTF_database[angle], angle, 1);
        }
    }

    public void start() {
        if (mIsPlaying) return;
        mIsPlaying = true;

        mAudioTrack.play();
        mProvider.startProviding();
        mConsumer.startConsuming();
//        mSoundOrbit.startRunning();

        try {
            mProvider.setPriority(7);
            mProvider.start();
        } catch (Exception ignored) {
        } finally {
            Log.i(TAG, "start: mProvider started");
        }

        try {
            mConsumer.setPriority(7);
            mConsumer.start();
        } catch (Exception ignored) {
        } finally {
            Log.i(TAG, "start: mConsumer started");
        }

//        try {
//            mSoundOrbit.setPriority(7);
//            mSoundOrbit.start();
//        } catch (Exception ignored) {
//        } finally {
//            Log.i(TAG, "start: mSoundOrbit started");
//        }

        Log.i(TAG, "start: thread started");

    }

    public void stop() {
        mProvider.stopProviding();
        mConsumer.stopConsuming();
//        mSoundOrbit.stopRunning();
        mIsPlaying = false;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public int makeNewSO(Point2D SOPoint, float[] soundArray) {
        int SOHandle;
        int x_size_j = 1000;
        SOHandle = initSoundObject(x_size_j,
                SOPoint.x,
                SOPoint.y,
                soundArray);
        SPOHandleList[DEFAULT_SO_HANDLE] = SOHandle;
        return SOHandle;
    }


    public void setSoundObjectView(int SOhandle, SoundObjectView soundObjectView) {
        mSoundOrbit.setOrbitView(soundObjectView);
    }


    public void setSOOrbitView(int SOhandle, SoundObjectView view) {
        mSoundOrbit.setOrbitView(view);
    }

    public void startSOOrbit(int SOHandle) {
        mSoundOrbit.startRunning();
    }

    public void stopSOOrbit(int SOhandle) {
        mSoundOrbit.stopRunning();
    }


    private native void loadHRTF(float[] HRTF_database_j, int angleIndex_j, int channel);

    private native int initSoundObject(int x_size_j, float x_j, float y_j, float[] sound_j);

    public native void loadSound(int handle_j, float[] sound_j);


    public native void setSOAngle(int handle_j, float angle_j);

    public native float getSOAngle(int handle_j);

    public native void setSODistance(int handle_j, float distance_j);

    public native float getSODistance(int handle_j);

    public native void getSOPoint(int SOHandle_j, Point2D p_j);

    public native void setSOPoint(int SOHandle_j, Point2D p_j);

    public native void getSOCenterPoint(int SOHandle_j, Point2D p_j);

    public native void setSOCenterPoint(int SOHandle_j, Point2D p_j);

    public native void getSOStartPoint(int SOHandle_j, Point2D p_j);

    public native void setSOStartPoint(int SOHandle_j, Point2D p_j);

    public native void getSOEndPoint(int SOHandle_j, Point2D p_j);

    public native void setSOEndPoint(int SOHandle_j, Point2D p_j);

    public native float getSORadius(int SOHandle_j);

    public native void setSORadius(int SOHandle_j, float radius_j);

    public native float getSOCenterAngle(int SOHandle_j);


    public native int getOrbitMode(int SOHandle_j);

    public native void setOrbitMode(int SOHandle_j, int mode_j);

    public SoundOrbit getSoundOrbit(int SOHandle) {
        return mSoundOrbit;
    }
}


