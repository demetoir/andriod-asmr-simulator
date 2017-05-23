package me.demetoir.a3dsound_ndk;

import android.media.AudioTrack;
import android.util.Log;

/**
 * Created by Yujun-desktop on 2017-05-24.
 */

public class SoundEngine {
    private final static String TAG = "SoundEngine";

    private SoundProvider mProvider;
    private SoundConsumer mConsummer;
    private SoundBuffer mSoundBuffer;
    private AudioTrack mAudioTrack;



    SoundEngine(float[] LeftHRTF,
                float[] RightHRTF,
                float[] inputSound,
                AudioTrack audioTrack) {

        mAudioTrack = audioTrack;
        mSoundBuffer = new SoundBuffer();

        mConsummer = new SoundConsumer(mSoundBuffer, mAudioTrack);
        mProvider = new SoundProvider(mSoundBuffer, LeftHRTF, RightHRTF, inputSound);
    }

    public void start() {
        mAudioTrack.play();
        mConsummer.playSound();

        mProvider.start();
        mConsummer.start();
        Log.i(TAG, "start: thread started");

        try {
            mProvider.join();

            mConsummer.stopSound();
            mConsummer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.i(TAG, "start: thread error");
        }
        Log.i(TAG, "start: thread run success");


        mConsummer.stopSound();
        mAudioTrack.stop();

    }
}
