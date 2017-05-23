package me.demetoir.a3dsound_ndk;

import java.nio.FloatBuffer;


class SoundBuffer {
    private final static String TAG = "SoundBuffer";

    final static int BUFFER_SIZE = 2048 * 32;
    final static int PUSHABLE_SIZE = 128;
    private final static int POPABLE_SIZE = 128;

    private FloatBuffer mBuffer;

    SoundBuffer() {
        mBuffer = FloatBuffer.allocate(BUFFER_SIZE * 2);
    }

    SoundBuffer(FloatBuffer mBuffer) {
        this.mBuffer = mBuffer;
    }

    boolean isPushalbe() {
        return this.mBuffer.hasRemaining();
    }

    boolean isPopable() {
        return this.mBuffer.position() > POPABLE_SIZE;
    }

    public float[] popBuffer() {
        float[] floats = new float[POPABLE_SIZE];
        synchronized (this) {
            mBuffer.flip();
            mBuffer.get(floats, 0, POPABLE_SIZE);
            mBuffer.compact();
        }
        return floats;
    }

    public void pushBuffer(float[] floats) {
        synchronized (this) {
            this.mBuffer.put(floats);
        }
    }
}
