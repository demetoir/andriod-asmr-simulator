package me.demetoir.a3dsound_ndk;

class SoundProvider extends Thread {
    private final static String TAG = "SoundProvider";

    static {
        System.loadLibrary("native-lib");
    }

    private SoundBuffer mSoundBuffer;
    private int mSOHandle;
    private boolean mIsProviding;

    SoundProvider(SoundBuffer object, int SOHandle) {
        mSoundBuffer = object;
        mSOHandle = SOHandle;
        mIsProviding = false;
    }

    private void providerProcess() {
//        Log.i(TAG, "providerProcess: start");
        while (true) {
            if (!mIsProviding) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
//            long start = System.currentTimeMillis();

            if (!mSoundBuffer.isPushAble()) continue;

            synchronized (this) {
                mSoundBuffer.pushBuffer(convProcess(mSOHandle));
            }

//            long end = System.currentTimeMillis();
//            Log.i(TAG, "providerProcess: time " + (end - start) / 1000.0);
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

    public native float[] convProcess(int SOHandle_j);

}
