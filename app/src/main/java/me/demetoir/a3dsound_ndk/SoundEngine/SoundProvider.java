package me.demetoir.a3dsound_ndk.SoundEngine;

import java.nio.ByteBuffer;

class SoundProvider extends Thread {
    private final static String TAG = "SoundProvider";
    private final static int THREAD_WAKE_UP_TIME = 100;

    static {
        System.loadLibrary("native-lib");
    }

    private SoundBuffer mSoundBuffer;
    private int mSOHandle;
    private boolean mIsProviding;
    private boolean mIsExitThread;

    SoundProvider(SoundBuffer object, int SOHandle) {
        mSoundBuffer = object;
        mSOHandle = SOHandle;
        mIsProviding = false;
        mIsExitThread = false;
    }

    private void providerProcess() {
        while (!mIsExitThread) {
            if (!mIsProviding) {
                try {
                    sleep(THREAD_WAKE_UP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (mSoundBuffer.isPushAble()) {
                mSoundBuffer.pushBuffer(signalProcess(mSOHandle));
                continue;
            }

            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        super.run();
        providerProcess();
    }

    void startProviding() {
        mIsProviding = true;
    }

    void stopProviding() {
        mIsProviding = false;
    }

    public native float[] signalProcess(int SOHandle_j);

    public native void bypassSignalProcess(int SOHandle_j, ByteBuffer buf_j, int buf_start_index_j);

}
