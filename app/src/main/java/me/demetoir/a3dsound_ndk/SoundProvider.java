package me.demetoir.a3dsound_ndk;

class SoundProvider extends Thread {
    private final static String TAG = "SoundProvider";
    private final static int THREAD_WAKE_UP_TIME = 100;
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
        while (true) {
            if (!mIsProviding) {
                try {
                    sleep(THREAD_WAKE_UP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (!mSoundBuffer.isPushAble()) continue;

            synchronized (this) {
                mSoundBuffer.pushBuffer(signalProcess(mSOHandle));
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

}
