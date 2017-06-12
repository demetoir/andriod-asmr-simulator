package me.demetoir.a3dsound_ndk;

import android.media.AudioTrack;
import android.util.Log;


class SoundEngine {
    private final static String TAG = "SoundEngine";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private final static int MAX_SPOHANDLE_SIZE = 10;
    private final static int DEFAULT_SO_HANDLE = 0;

    private SoundProvider mProvider;
    private SoundConsumer mConsumer;
    private SoundBuffer mSoundBuffer;
    private AudioTrack mAudioTrack;
    private soundOrbit mSoundOrbit;

    private int[] SPOHandleList;
    private boolean mIsPlaying;

    SoundEngine(AudioTrack audioTrack) {
        mAudioTrack = audioTrack;
        mSoundBuffer = new SoundBuffer();

        SPOHandleList = new int[MAX_SPOHANDLE_SIZE];

        mConsumer = new SoundConsumer(mSoundBuffer, mAudioTrack);
        mProvider = new SoundProvider(mSoundBuffer, DEFAULT_SO_HANDLE);
        mIsPlaying = false;

        mConsumer.addSoundProvider(mProvider);
        mSoundOrbit = new soundOrbit(this, DEFAULT_SO_HANDLE);
        mSoundOrbit.start();

    }

    void loadHRTF_database(float[][] rightHRTF_database,
                           float[][] leftHRTF_database,
                           int angleIndexSize) {
        for (int angle = 0; angle < angleIndexSize; angle++) {
            loadHRTF(rightHRTF_database[angle], angle, 0);
            loadHRTF(leftHRTF_database[angle], angle, 1);
        }
    }

    void start() {
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

    void stop() {
        mProvider.stopProviding();
        mConsumer.stopConsuming();
//        mSoundOrbit.stopRunning();
        mIsPlaying = false;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    int makeNewSO(int x_size_j, float x_j, float y_j, float[] sound_j) {
        int SOhandle = initSoundObject(x_size_j, x_j, y_j, sound_j);
        SPOHandleList[DEFAULT_SO_HANDLE] = SOhandle;
        return SOhandle;
    }


    void setSoundObjectView(int SOhandle, SoundObjectView soundObjectView) {
        mSoundOrbit.setOrbitView(soundObjectView);
    }


    void setSOOrbitView(int SOhandle, SoundObjectView view) {
        mSoundOrbit.setOrbitView(view);
    }

    public void startSOOrbit(int SOHandle){
        mSoundOrbit.startRunning();
    }
    public void stopSOOrbit(int SOhandle){
        mSoundOrbit.stopRunning();
    }


    private native void loadHRTF(float[] HRTF_database_j, int angleIndex_j, int channel);

    private native int initSoundObject(int x_size_j, float x_j, float y_j, float[] sound_j);

    public native void setSOAngle(int handle_j, float angle_j);

    public native float getSOAngle(int handle_j);

    public native void setSODistance(int handle_j, float distance_j);

    public native float getSODistance(int handle_j);

    public native void setSOX(int handle_j, float x_j);

    public native float getSOX(int handle_j);

    public native void setSOY(int handle_j, float y_j);

    public native float getSOY(int handle_j);


}
