package me.demetoir.a3dsound_ndk;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static me.demetoir.a3dsound_ndk.SoundBuffer.PUSHABLE_SIZE;

class SoundProvider extends Thread {
    private final static String TAG = "SoundProvider";
    private final static int THREAD_WAKE_UP_TIME = 100;
    float[] tempBuffer = new float[PUSHABLE_SIZE];

    static {
        System.loadLibrary("native-lib");
    }

    private SoundBuffer mSoundBuffer;
    private int mSOHandle;
    private boolean mIsProviding;
    ByteBuffer buf;

    SoundProvider(SoundBuffer object, int SOHandle) {
        mSoundBuffer = object;
        mSOHandle = SOHandle;
        mIsProviding = false;
        buf = ByteBuffer.allocateDirect(1024 * 8);
        buf.order(ByteOrder.LITTLE_ENDIAN);
    }

    private void providerProcess() {
        while (true) {
            if (!mIsProviding) {
                try {
                    sleep(THREAD_WAKE_UP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

//            Log.i(TAG, "providerProcess: getPushableSize = " + mSoundBuffer.getPushableSize());
            if (mSoundBuffer.isPushAble()) {
                mSoundBuffer.pushBuffer(signalProcess(mSOHandle));
//                bypassSignalProcess(mSOHandle, buf, buf.position());
//                buf.asFloatBuffer().get(tempBuffer);
//                mSoundBuffer.pushBuffer(tempBuffer);
                continue;
            }

            try {
                sleep(0,1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            synchronized (this) {
//                try {
//                    this.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
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
