package me.demetoir.a3dsound_ndk;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private final static double MAX_ANGLE = 360;
    private final static int TOTAL_ANGLE_STEP = 20;
    private final static int SAMPLING_RATE = 44100;
    private final static double MAX_DISTANCE = 10;
    private final static int BUFFER_SIZE = 2048;
    private final static int MAX_ANGLE_INDEX_SIZE = 100;
    private final static int HTRF_SIZE = 200;
    private final static int MAX_SOHANDLE_SIZE = 10;

    private TextView mAngleTextView;
    private TextView mDistanceTextView;

    private double mAngle;
    private double mDistance;
    private float[] mSoundArray;

    private AudioTrack mAudioTrack;
    private SoundEngine mSoundEngine;
    private int[] mSOHandleList;

    // BUTTTON
    private Button mPlayBtn;
    private Button mStopBtn;
    private Button mCCWBtn;
    private Button mCWBtn;
    private Button mDistIncBtn;
    private Button mDistDecBtn;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initButtons();
        initTextView();

        mSoundArray = loadMonoSound(R.raw.raw_devil);

        initAudioTrack();

        mSOHandleList = new int[MAX_SOHANDLE_SIZE];
        mSoundEngine = new SoundEngine(mAudioTrack);
        mSoundEngine.loadHRTF_database(
                loadHRTFdatabase(R.raw.left_hrtf_database),
                loadHRTFdatabase(R.raw.right_hrtf_database),
                MAX_ANGLE_INDEX_SIZE);

        mSOHandleList[0] = mSoundEngine.makeNewSO(1000, 50, 15, mSoundArray);

    }

    private void initTextView() {
        mAngleTextView = (TextView) findViewById(R.id.angleText);
        mAngle = 50;
        updateAngleTextView();

        mDistanceTextView = (TextView) findViewById(R.id.distanceTextView);
        mDistance = 15;
        updateDistanceTextView();
    }

    private void initButtons() {
        mPlayBtn = (Button) findViewById(R.id.playButton);
        mPlayBtn.setOnClickListener(mPlayBtnOnClickListener);

        mStopBtn = (Button) findViewById(R.id.stopButton);
        mStopBtn.setOnClickListener(mStopBtnOnClickListener);

        mCCWBtn = (Button) findViewById(R.id.moveCounterClockWise);
        mCCWBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int angleIdx = (int) mSoundEngine.getSOAngle(0);
                if (angleIdx - 4 >= 0) {
                    mSoundEngine.setSOAngle(0, angleIdx - 4);
                    mAngle = angleIdx - 4;
                    updateAngleTextView();
                }
            }
        });

        mCWBtn = (Button) findViewById(R.id.moveClockWise);
        mCWBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int angleIdx = (int) mSoundEngine.getSOAngle(0);
                if (angleIdx + 4 <= 99) {
                    mSoundEngine.setSOAngle(0, angleIdx + 4);
                    mAngle = angleIdx + 4;
                    updateAngleTextView();
                }
            }
        });

        mDistIncBtn = (Button) findViewById(R.id.distanceIncrease);
        mDistIncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double dist = mSoundEngine.getSODistance(0);
                if (dist + 2 <= 30 - 1) {
                    mSoundEngine.setSODistance(0, dist + 2);
                    mDistance = dist + 2;
                    updateDistanceTextView();
                }
            }
        });

        mDistDecBtn = (Button) findViewById(R.id.distanceDecrease);
        mDistDecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double dist = mSoundEngine.getSODistance(0);
                if (dist - 2 > 0) {
                    mSoundEngine.setSODistance(0, dist - 2);
                    mDistance = dist - 2;
                    updateDistanceTextView();
                }
            }
        });
    }

    private void updateAngleTextView() {
        mAngleTextView.setText(String.format(getResources().getString(R.string.textAngleFormat),
                mAngle - 50));
    }

    private void updateDistanceTextView() {
        mDistanceTextView.setText(String.format(getResources().getString(R.string.textDistanceFormat), mDistance));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void initAudioTrack() {
        //init m
//        int minSize = SAMPLING_RATE;
        int minSize = AudioTrack.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT);
        Log.i(TAG, "initAudioTrack: minsize " + minSize);
//        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
//                SAMPLING_RATE,
//                AudioFormat.CHANNEL_OUT_MONO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                minSize,
//                AudioTrack.MODE_STREAM);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT,
                minSize,
                AudioTrack.MODE_STREAM);

        mAudioTrack.setVolume(3.0f);
    }


    private Button.OnClickListener mPlayBtnOnClickListener = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onClick(View v) {
            mSoundEngine.start();
        }
    };

    private Button.OnClickListener mStopBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSoundEngine.stop();
        }
    };

    private int getAngleIndex() {
        return 50 - (int) ((mAngle * 49) / 90);
    }

    float[][] loadHRTFdatabase(int resId_HRTFDatabase) {
        float[][] HRTFdatabase = new float[MAX_ANGLE_INDEX_SIZE][HTRF_SIZE];

        InputStream inputStream = this.getResources().openRawResource(resId_HRTFDatabase);
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

    float[] loadMonoSound(int monoSoundResId) {
        short[] monoSound;
        InputStream inputStream = this.getResources().openRawResource(monoSoundResId);
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

        return shortToFloat(monoSound);
    }

    float[] shortToFloat(short[] shorts) {
        int size = shorts.length;
        float[] floats = new float[size];
        final float factor = 32767;
        for (int i = 0; i < size; i++) {
            floats[i] = ((float) shorts[i]) / factor;
        }
        return floats;
    }

}
