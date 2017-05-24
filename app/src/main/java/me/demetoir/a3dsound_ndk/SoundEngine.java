package me.demetoir.a3dsound_ndk;

import android.media.AudioTrack;
import android.util.Log;

/**
 * Created by Yujun-desktop on 2017-05-24.
 */

public class SoundEngine {
    private final static String TAG = "SoundEngine";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private final static int MAX_SPOHANDLE_SIZE = 10;

    private SoundProvider mProvider;
    private SoundConsumer mConsummer;
    private SoundBuffer mSoundBuffer;
    private AudioTrack mAudioTrack;

    private int[] SPOHandleList;
    private boolean mIsPlaying;

    SoundEngine(AudioTrack audioTrack) {
        mAudioTrack = audioTrack;
        mSoundBuffer = new SoundBuffer();

        SPOHandleList = new int[MAX_SPOHANDLE_SIZE];

        mConsummer = new SoundConsumer(mSoundBuffer, mAudioTrack);
        mProvider = new SoundProvider(mSoundBuffer, 0);
        mIsPlaying = false;


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
        if(mIsPlaying) return;
        mIsPlaying = true;

        mAudioTrack.play();
        mProvider.startProviding();
        mConsummer.startConsumming();
        try {
            mProvider.start();
        }catch (Exception e){
        }

        try{
            mConsummer.start();
        }catch (Exception e){
        }


        Log.i(TAG, "start: thread started");

    }

    // TODO test please
    public void stop() {
        mProvider.stopProviding();
        mConsummer.stopConsumming();
        mIsPlaying = false;
    }


    public int makeNewSO(int x_size_j, double angle_j, double distance_j, float[] sound_j){
        int SOhandle = initSoundObject(x_size_j, angle_j, distance_j, sound_j);
        SPOHandleList[0] = SOhandle;
        return SOhandle;
    }

    private native void loadHRTF(float[] HRTF_database_j, int angleIndex_j, int channel);

    private native int initSoundObject(int x_size_j, double angle_j, double distance_j, float[] sound_j);

    public native void setSOAngle(int handle_j, int angle_j);

    public native int getSOAngle(int handle_j);

    public native void setSODistance(int handle_j, int distance_j);

    public native int getSODistance(int handle_j);
}
