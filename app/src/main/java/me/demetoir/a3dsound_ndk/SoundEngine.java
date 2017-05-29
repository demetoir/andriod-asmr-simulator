package me.demetoir.a3dsound_ndk;

        import android.media.AudioTrack;
        import android.util.Log;


class SoundEngine {
    private final static String TAG = "SoundEngine";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private final static int MAX_SPOHANDLE_SIZE = 10;

    private SoundProvider mProvider;
    private SoundConsumer mConsumer;
    private SoundBuffer mSoundBuffer;
    private AudioTrack mAudioTrack;

    private int[] SPOHandleList;
    private boolean mIsPlaying;

    SoundEngine(AudioTrack audioTrack) {
        mAudioTrack = audioTrack;
        mSoundBuffer = new SoundBuffer();

        SPOHandleList = new int[MAX_SPOHANDLE_SIZE];

        mConsumer = new SoundConsumer(mSoundBuffer, mAudioTrack);
        mProvider = new SoundProvider(mSoundBuffer, 0);
        mIsPlaying = false;
    }

    public void loadHRTF_database(float[][] rightHRTF_database,
                                  float[][] leftHRTF_database,
                                  int angleIndexSize) {
        for (int angle = 0; angle < angleIndexSize; angle++) {
            loadHRTF(rightHRTF_database[angle], angle, 0);
            loadHRTF(leftHRTF_database[angle], angle, 1);
        }
    }

    public void start() {
        if (mIsPlaying) return;
        mIsPlaying = true;

        mAudioTrack.play();
        mProvider.startProviding();
        mConsumer.startConsumming();
        try {
            mProvider.setPriority(7);
            mProvider.start();
        } catch (Exception ignored) {
        } finally {
            Log.i(TAG, "start: mProvider started");
        }

        try {
            mConsumer.setPriority(7);
            mConsumer.start();
        } catch (Exception ignored) {
        } finally {
            Log.i(TAG, "start: mConsumer started");
        }


        Log.i(TAG, "start: thread started");

    }

    // TODO test please
    public void stop() {
        mProvider.stopProviding();
        mConsumer.stopConsumming();
        mIsPlaying = false;
    }


    public int makeNewSO(int x_size_j, double x_j, double y_j, float[] sound_j) {
        int SOhandle = initSoundObject(x_size_j, x_j, y_j, sound_j);
        SPOHandleList[0] = SOhandle;
        return SOhandle;
    }

    private native void loadHRTF(float[] HRTF_database_j, int angleIndex_j, int channel);

    private native int initSoundObject(int x_size_j, double x_j, double y_j, float[] sound_j);

    public native void setSOAngle(int handle_j, double angle_j);

    public native double getSOAngle(int handle_j);

    public native void setSODistance(int handle_j, double distance_j);

    public native double getSODistance(int handle_j);

    public native void setSOX(int handle_j, double x_j);

    public native double  getSOX( int handle_j);

    public native void setSOY(int handle_j, double y_j);

    public native double getSOY(int handle_j);
}
