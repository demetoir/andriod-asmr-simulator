package me.demetoir.a3dsound_ndk;

import android.media.AudioTrack;
import android.os.Build;
import android.support.annotation.RequiresApi;


class SoundConsumer extends Thread {
    private final static String TAG = "SoundConsumer";

    private final static int THREAD_WAKE_UP_TIME = 100;
    static {
        System.loadLibrary("native-lib");
    }

    SoundBuffer mSoundBuffer;
    AudioTrack mAudioTrack;
    boolean mIsConsuming;
    SoundProvider mSoundProvider;

    SoundConsumer(SoundBuffer mSoundBuffer, AudioTrack audioTrack) {
        this.mSoundBuffer = mSoundBuffer;
        this.mAudioTrack = audioTrack;
        this.mIsConsuming = false;
    }

    void addSoundProvider(SoundProvider soundProvider){
        mSoundProvider = soundProvider;
    }

    void startConsuming() {
        mIsConsuming = true;
    }

    void stopConsuming() {
        mIsConsuming = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void soundProcess() {
        float[] outputSound;
        while (true) {
            if (!mIsConsuming){
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

            outputSound = mSoundBuffer.popBuffer();
            mAudioTrack.write(
                    outputSound,
                    0,
                    outputSound.length,
                    AudioTrack.WRITE_BLOCKING);
//            synchronized (mSoundProvider) {
//                mSoundProvider.notify();
//            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        super.run();
        soundProcess();
    }
}
