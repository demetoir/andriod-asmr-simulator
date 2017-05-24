package me.demetoir.a3dsound_ndk;

import android.media.AudioTrack;
import android.util.Log;


class SoundConsumer extends Thread {
    private final static String TAG = "SoundConsumer";

    SoundBuffer mSoundBuffer;
    AudioTrack mAudioTrack;
    boolean mIsConsumming;

    SoundConsumer(SoundBuffer mSoundBuffer, AudioTrack audioTrack) {
        this.mSoundBuffer = mSoundBuffer;
        this.mAudioTrack = audioTrack;
        this.mIsConsumming = false;

    }

    void startConsumming() {
        mIsConsumming = true;
    }

    void stopConsumming() {
        mIsConsumming = false;
    }

    private void soundProcess() {
        float[] outputSound;
        while (true) {
            if (!mIsConsumming){
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (!mSoundBuffer.isPopable()) {
                continue;
            }

            long end = System.currentTimeMillis();
            outputSound = mSoundBuffer.popBuffer();
            mAudioTrack.write(outputSound, 0, outputSound.length,
                    AudioTrack.WRITE_NON_BLOCKING);
            long start = System.currentTimeMillis();
            Log.i(TAG, "cunsumProcess: time = " + (end - start) / 1000.0);


        }
    }

    @Override
    public void run() {
        super.run();
        soundProcess();
    }
}
