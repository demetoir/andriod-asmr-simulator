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

import me.demetoir.a3dsound_ndk.R;
import me.demetoir.a3dsound_ndk.util.Util;

import static android.content.ContentValues.TAG;

public class SoundBank {
    public final static int DEFAULT_SAMPLING_RATE = 44100;
    public final static float DEFAULT_VOLUME = 3.0f;

    public final static int SOUND_GLASS_BOTTLE = R.raw.sound_glass_bottle;
    public final static int SOUND_HAND = R.raw.sound_hand;
    public final static int sound_latex_glove = R.raw.sound_latex_glove;
    public final static int SOUND_NOTE_BOOK_TYPING = R.raw.sound_note_book_typing;
    public final static int SOUND_SCISSORS = R.raw.sound_scissors;
    public final static int SOUND_WATER_BOTTLE = R.raw.sound_water_bottle;
    public final static int SOUND_WOOD_BLOCK = R.raw.sound_wood_block;
    public final static int SOUND_PAPER = R.raw.sound_paper;

    public final static int SOUND_BANK_SIZE = 8;
    public final static String[] soundBankString = new String[SOUND_BANK_SIZE];

    public final static int DEFAULT_SOUND = SOUND_SCISSORS;

    private Activity mMainActivity;

    public SoundBank(Activity activity) {
        mMainActivity = activity;
    }

    public float[] getMonoSound(int monoSoundResId) {
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
//            e.printStackTrace();
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

    public int[] getSoundResList() {
        int[] list = new int[SOUND_BANK_SIZE];
        list[0] = SOUND_PAPER;
        list[1] = SOUND_GLASS_BOTTLE;
        list[2] = SOUND_HAND;
        list[3] = sound_latex_glove;
        list[4] = SOUND_NOTE_BOOK_TYPING;
        list[5] = SOUND_SCISSORS;
        list[6] = SOUND_WATER_BOTTLE;
        list[7] = SOUND_WOOD_BLOCK;
        return list;
    }

    public int[] getSoundStrIdList() {
        int[] list = new int[SOUND_BANK_SIZE];
        list[0] = R.string.sound_paper;
        list[1] = R.string.sound_glass_bottle;
        list[2] = R.string.sound_hand;
        list[3] = R.string.sound_latex_glove;
        list[4] = R.string.sound_note_book_typing;
        list[5] = R.string.sound_scissors;
        list[6] = R.string.sound_water_bottle;
        list[7] = R.string.sound_wood_block;
        return list;
    }

    public String[] getSoundString() {
        int[] idList = getSoundStrIdList();
        String[] soundString = new String[idList.length];
        for (int i = 0; i < idList.length; i++) {
            soundString[i] = mMainActivity.getResources().getString(idList[i]);
        }
        return soundString;
    }

    public int getSoundResId(int index) {
        return getSoundResList()[index];
    }

    // TODO imp ?
    private void soundSourceCheck() {
        String strbellPath = "/data/data/me.demetoir.a3dsound_ndk/files/raw_devil.snd";
        try {
            CopyIfNotExist(R.raw.raw_devil, strbellPath);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void CopyIfNotExist(int resID, String target) throws IOException {
        File targetFile = new File(target);
        if (!targetFile.exists()) {
            Log.i(TAG, "CopyIfNotExist: file not exist");
            CopyFromPackage(resID, targetFile.getName());
        } else {
            Log.i(TAG, "CopyIfNotExist: file exist");
        }
    }

    private void CopyFromPackage(int resID, String target) throws IOException {
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
