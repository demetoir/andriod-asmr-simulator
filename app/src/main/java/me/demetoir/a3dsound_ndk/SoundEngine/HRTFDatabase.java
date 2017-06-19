package me.demetoir.a3dsound_ndk.SoundEngine;


import android.app.Activity;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import me.demetoir.a3dsound_ndk.MainActivity;
import me.demetoir.a3dsound_ndk.R;

import static android.content.ContentValues.TAG;

public class HRTFDatabase {
    public final static int MAX_ANGLE_INDEX_SIZE = 100;
    private final static int HRTF_SIZE = 200;

    private MainActivity mMainActivity;

    public HRTFDatabase(Activity activity) {
        mMainActivity = (MainActivity) activity;
    }

    private float[][] loadHRTFdatabase(int resId_HRTFDatabase) {
        float[][] HRTFdatabase = new float[MAX_ANGLE_INDEX_SIZE][HRTF_SIZE];

        InputStream inputStream = mMainActivity.getResources().openRawResource(resId_HRTFDatabase);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        try {
            for (int index = 0; index < MAX_ANGLE_INDEX_SIZE; index++) {
                String line = bufferedReader.readLine();
                String[] values = line.split("\t");
                for (int angle = 0; angle < values.length; angle++) {
                    HRTFdatabase[angle][index] = Float.parseFloat(values[angle]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "loadHRTFdatabase: ", e);
        }

        try {
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "loadHRTFdatabase: ", e);
        }

        Log.i(TAG, "loadHRTFdatabase: load end");
        return HRTFdatabase;
    }

    public float[][] leftHRTFDatabase(){
        return loadHRTFdatabase(R.raw.left_hrtf_database);
    }

    public float[][] rightHRTFDatabase(){
        return loadHRTFdatabase(R.raw.right_hrtf_database);
    }

}
