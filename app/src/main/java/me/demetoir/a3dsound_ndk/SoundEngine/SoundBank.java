package me.demetoir.a3dsound_ndk.SoundEngine;




import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import me.demetoir.a3dsound_ndk.MainActivity;
import me.demetoir.a3dsound_ndk.R;
import me.demetoir.a3dsound_ndk.util.Util;

import static android.content.ContentValues.TAG;

public class SoundBank {
    public final static int SAMPLING_RATE = 44100;
    public final static float DEFAULT_VOLUME = 3.0f;

    public final static int SOUND_BIRD = R.raw.sound_bird;
    public final static int SOUND_GLASS_BOTTLE = R.raw.sound_glass_bottle;
    public final static int SOUND_HAND = R.raw.sound_hand;
    public final static int sound_latex_glove = R.raw.sound_latex_glove;
    public final static int SOUND_NOTE_BOOK_TYPING = R.raw.sound_note_book_typing;
    public final static int SOUND_SCISSORS = R.raw.sound_scissors;
    public final static int SOUND_WATER_BOTTEL = R.raw.sound_water_bottle;
    public final static int SOUND_WOOD_BLOCK = R.raw.sound_wood_block;
    public final static int SOUND_PAPER = R.raw.sound_paper;

    public final static int SOUND_BANK_SIZE = 9;
    public final static String[] soundBankString = new String[SOUND_BANK_SIZE];

    public final static int DEFAULT_SOUND = SOUND_SCISSORS;

    private MainActivity mMainActivity;

    public SoundBank(Activity activity) {
        mMainActivity = (MainActivity) activity;
    }

    public float[] loadMonoSound(int monoSoundResId) {
        short[] monoSound;
        InputStream inputStream = mMainActivity.getResources().openRawResource(monoSoundResId);
        DataInputStream dataInputStream = null;
        ArrayList<Short> shorts = new ArrayList<>();
        try {
            dataInputStream = new DataInputStream(inputStream);
            while (true) {
                shorts.add(dataInputStream.readShort());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "loadSound: " + e);
        } finally {
            monoSound = new short[shorts.size()];
            int size = shorts.size();
            for (int i = 0; i < size; i++) {
                monoSound[i] = shorts.get(i);
            }
            Log.i(TAG, "loadSound: sound length : " + Integer.toString(size));
        }

        try {
            inputStream.close();
            dataInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Util.shortToFloat(monoSound);
    }


    private void soundSourceCheck() {
        String strbellPath = "/data/data/me.demetoir.a3dsound_ndk/files/raw_devil.snd";
        try {
            CopyIfNotExist(R.raw.raw_devil, strbellPath);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void CopyIfNotExist(int resID, String target) throws IOException {
        File targetFile = new File(target);
        if (!targetFile.exists()) {
            Log.i(TAG, "CopyIfNotExist: file not exist");
            CopyFromPackage(resID, targetFile.getName());
        } else {
            Log.i(TAG, "CopyIfNotExist: file exist");
        }
    }

    public void CopyFromPackage(int resID, String target) throws IOException {
        FileOutputStream lOutputStream = mMainActivity.openFileOutput(target, Context.MODE_PRIVATE);
        InputStream lInputStream = mMainActivity.getResources().openRawResource(resID);
        int readByte;
        byte[] buff = new byte[8048];

        while ((readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff, 0, readByte);
        }

        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }


}
