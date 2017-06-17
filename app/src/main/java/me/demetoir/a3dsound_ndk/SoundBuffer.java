package me.demetoir.a3dsound_ndk;

import java.nio.FloatBuffer;


class SoundBuffer {
    private final static String TAG = "SoundBuffer";

    private final static int BUFFER_SIZE = 1024*8 ;
    private final static int CHANNEL_SIZE = 2;
    private final static int PUSHABLE_SIZE_PER_CHANNEL = 32;
    public final static int PUSHABLE_SIZE = PUSHABLE_SIZE_PER_CHANNEL * CHANNEL_SIZE;
    private final static int POPABBLE_SIZE_PER_CHANNEL = 128;
    public final static int POPABLE_SIZE = POPABBLE_SIZE_PER_CHANNEL * CHANNEL_SIZE;

    private FloatBuffer mBuffer;
    private float[] mTempBuf;

    SoundBuffer() {
        mBuffer = FloatBuffer.allocate(BUFFER_SIZE);
        mTempBuf = new float[POPABLE_SIZE];
    }

    boolean isPushAble() {
        return this.mBuffer.remaining() >= PUSHABLE_SIZE;
    }

    boolean isPopAble() {
        return this.mBuffer.position() > POPABLE_SIZE;
    }

    public float[] popBuffer() {
        synchronized (this) {
            mBuffer.flip();
            mBuffer.get(mTempBuf, 0, POPABLE_SIZE);
            mBuffer.compact();
        }
        return mTempBuf;
    }

    public void pushBuffer(float[] floats) {
        synchronized (this) {
            mBuffer.put(floats);
        }
    }

    public int getPushableSize(){
        return this.mBuffer.remaining();
    }
    public FloatBuffer getmBuffer(){
        return mBuffer;
    }
}
