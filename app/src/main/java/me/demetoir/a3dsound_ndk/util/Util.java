package me.demetoir.a3dsound_ndk.util;


import me.demetoir.a3dsound_ndk.SoundEngine.SoundOrbit;

public class Util {
    public static float[] shortToFloat(short[] shorts) {
        int size = shorts.length;
        float[] floats = new float[size];
        final float factor = 32767;
        for (int i = 0; i < size; i++) {
            floats[i] = ((float) shorts[i]) / factor;
        }
        return floats;
    }

    public static int speedToProgress(float speed, int max) {
        return (int) ((speed - SoundOrbit.MIN_SPEED) * (float) max / SoundOrbit.MAX_SPEED);
    }

    public static float progressToSpeed(int progress, int max) {
        return (float) (((float) progress / (float) max) * SoundOrbit.MAX_SPEED
                + SoundOrbit.MIN_SPEED);
    }
}
