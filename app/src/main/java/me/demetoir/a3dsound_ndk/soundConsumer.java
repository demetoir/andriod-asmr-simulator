package me.demetoir.a3dsound_ndk;

import android.media.AudioTrack;
import android.util.Log;

import static me.demetoir.a3dsound_ndk.SoundBuffer.PUSHABLE_SIZE;


class SoundConsumer extends Thread {
    private final static String TAG = "SoundConsumer";

    SoundBuffer mSoundBuffer;
    AudioTrack mAudioTrack;
    boolean mIsSoundPlay;

    SoundConsumer(SoundBuffer mSoundBuffer, AudioTrack audioTrack) {
        this.mSoundBuffer = mSoundBuffer;
        this.mAudioTrack = audioTrack;
        this.mIsSoundPlay = false;

    }

    void playSound() {
        this.mIsSoundPlay = true;
    }

    void stopSound() {
        this.mIsSoundPlay = false;
    }

    synchronized private void soundProcess() {
        float[] outputSound = new float[PUSHABLE_SIZE];
        while (true) {
            if (!this.mIsSoundPlay) break;
            long start = System.currentTimeMillis();

            if (!mSoundBuffer.isPopable()) {
                Log.i(TAG, "soundProcess: delayed");

                try {
                    wait(60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                continue;
            }
            outputSound = mSoundBuffer.popBuffer();
            mAudioTrack.write(outputSound, 0, outputSound.length,
                    AudioTrack.WRITE_NON_BLOCKING);
            long end = System.currentTimeMillis();
            Log.i(TAG, "soundProcess: time = "+(end-start)/1000.0);


        }
    }

    @Override
    public void run() {
        super.run();
        soundProcess();
    }
}
