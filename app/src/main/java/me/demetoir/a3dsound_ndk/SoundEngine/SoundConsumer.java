package me.demetoir.a3dsound_ndk.SoundEngine;

import android.media.AudioTrack;
import android.os.Build;
import android.support.annotation.RequiresApi;


class SoundConsumer extends Thread {
    private final static String TAG = "SoundConsumer";

    private final static int THREAD_WAKE_UP_TIME = 100;

    static {
        System.loadLibrary("native-lib");
    }

    private SoundBuffer mSoundBuffer;
    private AudioTrack mAudioTrack;
    private boolean mIsConsuming;
    private boolean mIsExitThread;
    private boolean isRunning;
    private SoundProvider mSoundProvider;

    SoundConsumer(SoundBuffer soundBuffer, AudioTrack audioTrack) {
        mSoundBuffer = soundBuffer;
        mAudioTrack = audioTrack;
        mIsConsuming = false;
        mIsExitThread = false;
        isRunning = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        super.run();
        soundProcess();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void soundProcess() {
        float[] outputSound;
        while (!mIsExitThread) {
            if (!mIsConsuming) {
                isRunning =false;
                try {
                    sleep(THREAD_WAKE_UP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (!mSoundBuffer.isPopAble()) {
                continue;
            }

            isRunning = true;
            outputSound = mSoundBuffer.popBuffer();
            mAudioTrack.write(
                    outputSound,
                    0,
                    outputSound.length,
                    AudioTrack.WRITE_BLOCKING);
            synchronized (mSoundProvider) {
                mSoundProvider.notify();
            }
        }
    }

    void addSoundProvider(SoundProvider soundProvider) {
        mSoundProvider = soundProvider;
    }

    void startConsuming() {
        mIsConsuming = true;
    }

    void stopConsuming() {
        mIsConsuming = false;
        //wait for stop thread
        while (isRunning) {
        }
    }

    public boolean isRunning(){
        return isRunning;
    }
}
