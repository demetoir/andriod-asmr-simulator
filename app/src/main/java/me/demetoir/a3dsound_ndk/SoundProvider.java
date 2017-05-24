package me.demetoir.a3dsound_ndk;

import android.util.Log;

/**
 * Created by Yujun-desktop on 2017-05-24.
 */

class SoundProvider extends Thread {
    private final static String TAG = "SoundProvider";

    private final static int HRTF_SIZE = 200;

    static {
        System.loadLibrary("native-lib");
    }

    private SoundBuffer mSoundBuffer;
    float[] mLeftHRTF;
    float[] mRightHRTF;
    float[] mInputsound;
    int mSOHandle;
    boolean mIsProviding;

    SoundProvider(SoundBuffer object, int SOHandle) {
        mSoundBuffer = object;
        mSOHandle = SOHandle;
        mIsProviding = false;
    }

    short[] mixMonoToStereo(short[] leftMonoSound, short[] rightMonoSound) {
        int size = (leftMonoSound.length > rightMonoSound.length)
                ? leftMonoSound.length : rightMonoSound.length;
        short[] output = new short[size * 2];
        for (int i = 0; i < size * 2; i++)
            output[i] = 0;

        //left
        for (int i = 0; i < leftMonoSound.length; i++)
            output[i * 2] = leftMonoSound[i];

        //right
        for (int i = 0; i < rightMonoSound.length; i++)
            output[i * 2 + 1] = rightMonoSound[i];

        return output;
    }

    float[] mixMonoToStereo(float[] leftMonoSound, float[] rightMonoSound) {
        int size = (leftMonoSound.length > rightMonoSound.length)
                ? leftMonoSound.length : rightMonoSound.length;
        float[] output = new float[size * 2];
        for (int i = 0; i < size * 2; i++)
            output[i] = 0;

        //left
        for (int i = 0; i < leftMonoSound.length; i++)
            output[i * 2] = leftMonoSound[i];

        //right
        for (int i = 0; i < rightMonoSound.length; i++)
            output[i * 2 + 1] = rightMonoSound[i];

        return output;
    }

    public float[] conv(float x[], int head,
                        float[] leftOutput, float[] rightOutput) {
        float[] ret = new float[SoundBuffer.PUSHABLE_SIZE];

        for (int i = 0; i < SoundBuffer.PUSHABLE_SIZE; i++) {
            // delay and push
            for (int j = HRTF_SIZE - 1; j > 0; j--) {
                x[j] = x[j - 1];
            }

            x[0] = mInputsound[head + i];

            float leftSum = 0;
            float rightSum = 0;
            for (int j = 0; j < HRTF_SIZE; j++) {
                leftSum += x[j] * mLeftHRTF[j];
                rightSum += x[j] * mRightHRTF[j];
            }
            leftOutput[i] = leftSum;
            rightOutput[i] = rightSum;
        }

        // apply distance (change volume)

        // mix left right

        return ret;
    }


    public void providerProcess() {
        Log.i(TAG, "providerProcess: start");
        while (true) {
            if (!mIsProviding) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            long start = System.currentTimeMillis();
            float[] fMixOutput = convProcess(mSOHandle);

            if (!mSoundBuffer.isPushalbe()) continue;

            mSoundBuffer.pushBuffer(fMixOutput);
            long end = System.currentTimeMillis();
            Log.i(TAG, "providerProcess: time " + (end - start) / 1000.0);
        }
    }

    @Override
    public void run() {
        super.run();
        providerProcess();
    }

    public native float[] convProcess(int SOHandle_j);

    public void startProviding() {
        mIsProviding = true;
    }

    public void stopProviding() {
        mIsProviding = false;
    }

}
