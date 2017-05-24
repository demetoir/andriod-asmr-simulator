package me.demetoir.a3dsound_ndk;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
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

    private StartBtnAsyncTask mStartBtnTask;
    private StopBtnAsyncTask mStopBtntask;

    public class StartBtnAsyncTask extends AsyncTask<Void, Void, Void> {
        final static String TAG = "StartBtnAsyncTask";
        SoundEngine mSoundEngine;
        public StartBtnAsyncTask(SoundEngine soundEngine) {
            mSoundEngine = soundEngine;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mSoundEngine.start();
            Log.i(TAG, "doInBackground: end task");
            return null;
        }

    }

    public class StopBtnAsyncTask extends AsyncTask<Void, Void, Void> {
        final static String TAG = "StopBtnAsyncTask";
        SoundEngine mSoundEngine;

        public StopBtnAsyncTask(SoundEngine soundEngine) {
            mSoundEngine = soundEngine;
            Log.i(TAG, "StopBtnAsyncTask: end task");
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.i(TAG, "doInBackground: stop Sound");

            mSoundEngine.stop();
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initButtons();
        initTextView();

        mSoundArray = loadMonoSound(R.raw.raw_explosion);

        initAudioTrack();

        mSOHandleList = new int[MAX_SOHANDLE_SIZE];
        mSoundEngine = new SoundEngine(mAudioTrack);
        mSoundEngine.loadHRTF_database(
                loadHRTFdatabase(R.raw.left_hrtf_database),
                loadHRTFdatabase(R.raw.right_hrtf_database),
                MAX_ANGLE_INDEX_SIZE);

        int SOHandle = mSoundEngine.makeNewSO(64, mAngle, mDistance, mSoundArray);

        mStartBtnTask = new StartBtnAsyncTask(mSoundEngine);
        mStopBtntask = new StopBtnAsyncTask(mSoundEngine);
    }

    private void initTextView() {
        mAngleTextView = (TextView) findViewById(R.id.angleText);
        mAngle = 0;
        updateAngleTextView();

        mDistanceTextView = (TextView) findViewById(R.id.distanceTextView);
        mDistance = 0.0;
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
                if (mAngle > -90) {
                    mAngle -= 10;
                    updateAngleTextView();
                }
            }
        });

        mCWBtn = (Button) findViewById(R.id.moveClockWise);
        mCWBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAngle < 90) {
                    mAngle += 10;
                    updateAngleTextView();
                }
            }
        });

        mDistIncBtn = (Button) findViewById(R.id.distanceIncrease);
        mDistIncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDistance < MAX_DISTANCE) {
                    mDistance += 1;
                    updateDistanceTextView();
                }
            }
        });

        mDistDecBtn = (Button) findViewById(R.id.distanceDecrease);
        mDistDecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDistance > 0) {
                    mDistance -= 1;
                    updateDistanceTextView();
                }
            }
        });
    }

    private void updateAngleTextView() {
        mAngleTextView.setText(String.format(getResources().getString(R.string.textAngleFormat), mAngle));
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
//            testPlaySound();
            mSoundEngine.start();
//            StartBtnAsyncTask task = new StartBtnAsyncTask(mSoundEngine);
//
//            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
//                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//            } else {
//                task.execute();
//            }
//            int head = 0;
//            while (true) {
//                if (head >= sizeSound)
//                    break;
//
//                short[] mBuffer = new short[bufSize];
//                for (int i = 0; i < bufSize; i++) {
//                    if (head + i >= sizeSound)
//                        mBuffer[i] = 0;
//                    else
//                        mBuffer[i] = mSoundArray[head + i];
//                }
//
//                float[] leftOutput = filteringMonoSound(shortToComplex(mBuffer), mLeftHRTFdatabase[angleIdx]);
//                Log.i(TAG, "onClick: left filter end");
//                float[] rightOutput = filteringMonoSound(shortToComplex(mBuffer), mRightHRTFdatabase[angleIdx]);
//                Log.i(TAG, "onClick: right filter end");
//
//                // result two mono  sound to make stereo
//                short[] mixedSound = mixMonoToStereo(
//                        cutOff(floatToShort(leftOutput), bufSize),
//                        cutOff(floatToShort(rightOutput), bufSize)
//                );
//
//                for (int i = 0; i < mixedSound.length; i++) {
//                    outputSound[i + head * 2] = mixedSound[i];
//                }
//
//                Log.i(TAG, "onClick: head = " + head);
//                head += bufSize * 2;
//            }

            //한번에 필터링하는경우
//            int twoSize = 1;
//            while (twoSize <= mSoundArray.length + 200) {
//                twoSize *= 2;
//            }
//            short[] mBuffer = new short[twoSize];
//            for (int i = 0; i < mSoundArray.length; i++) {
//                mBuffer[i] = mSoundArray[i];
//            }
//
//            //int angleIdx = 50 - (int) ((mAngle * 49) / 90);
//            Log.i(TAG, "onClick: angleIdx = "+ angleIdx);
//            float[] leftOutput = filteringMonoSound(shortToComplex(mBuffer), mLeftHRTFdatabase[angleIdx]);
//            Log.i(TAG, "onClick: left filter end");
//            float[] rightOutput = filteringMonoSound(shortToComplex(mBuffer), mRightHRTFdatabase[angleIdx]);
//            Log.i(TAG, "onClick: right filter end");
//
//            short[] outputSound = mixMonoToStereo(
//                    cutOff(floatToShort(leftOutput), mSoundArray.length),
//                    cutOff(floatToShort(rightOutput), mSoundArray.length)
//            );
//            Log.i(TAG, "onClick: end convolutioin");
//            Log.i(TAG, "onClick: output len  = " + outputSound.length);
//            mAudioTrack.stop();
//            mAudioTrack.flush();
//            int len = mAudioTrack.write(outputSound, 0, outputSound.length);
//            Log.i(TAG, "onClick: len = " + len);
//            mAudioTrack.play();
//
//            int cursor = outputSound.length - len;
//            Log.i(TAG, "onClick: cursor = " + cursor);
//            while (cursor < outputSound.length) {
//                len = mAudioTrack.write(outputSound, cursor, outputSound.length - cursor);
//                mAudioTrack.play();
//                cursor += len;
//                Log.i(TAG, "onClick: cursor = " + cursor);
//            }

//            short[] mixedSound = mixMonoToStereo(mSoundArray, mSoundArray);
//            mAudioTrack.write(mixedSound, 0, mixedSound.length);
//            mAudioTrack.play();
//            Log.i(TAG, "onClick: len = ");
        }
    };

    private Button.OnClickListener mStopBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        mSoundEngine.stop();
//            StopBtnAsyncTask task = new StopBtnAsyncTask (mSoundEngine);
//
//            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
//                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//            } else {
//                task.execute();
//            }

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
//                Log.i(TAG, "loadHRTFdatabase: "+line);
//                for(int i =0; i<values.length; i++){
//                    Log.i(TAG, "loadHRTFdatabase: value i = " + i + " val = "+ values[i]);
//                }
                for (int angle = 0; angle < values.length; angle++) {
                    HRTFdatabase[angle][index] = Float.parseFloat(values[angle]);
//                    Log.i(TAG, "loadHRTFdatabase:  values  = " + values[angle]
//                            + " parse val" + Double.parseDouble(values[angle]));
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

//        for (int i = 0; i < 200; i++)
//            Log.i(TAG, "loadHRTFdatabase:  val = " + HRTFdatabase[0][i]);

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
//            for (int i = 0; i < 100; i++)
//                Log.i(TAG, "loadSound: index = " + i + " val = " + monoSound[i]);
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
    //
//    short[] mixMonoToStereo(short[] leftMonoSound, short[] rightMonoSound) {
//        int size = (leftMonoSound.length > rightMonoSound.length)
//                ? leftMonoSound.length : rightMonoSound.length;
//        short[] output = new short[size * 2];
//        for (int i = 0; i < size * 2; i++)
//            output[i] = 0;
//
//        //left
//        for (int i = 0; i < leftMonoSound.length; i++)
//            output[i * 2] = leftMonoSound[i];
//
//        //right
//        for (int i = 0; i < rightMonoSound.length; i++)
//            output[i * 2 + 1] = rightMonoSound[i];
//
//        return output;
//    }
//
//    float[] mixMonoToStereo(float[] leftMonoSound, float[] rightMonoSound) {
//        int size = (leftMonoSound.length > rightMonoSound.length)
//                ? leftMonoSound.length : rightMonoSound.length;
//        float[] output = new float[size * 2];
//        for (int i = 0; i < size * 2; i++)
//            output[i] = 0;
//
//        //left
//        for (int i = 0; i < leftMonoSound.length; i++)
//            output[i * 2] = leftMonoSound[i];
//
//        //right
//        for (int i = 0; i < rightMonoSound.length; i++)
//            output[i * 2 + 1] = rightMonoSound[i];
//
//        return output;
//    }
//
//
//    short[] setVolumeBy(short[] monoSound, double distance) {
//        int size = monoSound.length;
//        short[] outputSound = new short[size];
//        for (int i = 0; i < size; i++) {
//            outputSound[i] = (short) ((double) monoSound[i] * (distance / MAX_DISTANCE));
//        }
//
//        return outputSound;
//    }
//
//    short[] volumeUp(short[] shorts, double gain) {
//        int size = shorts.length;
//        short[] output = new short[size];
//        for (int i = 0; i < size; i++) {
//            double val = (double) shorts[i] * gain;
//            output[i] = (short) val;
//        }
//        return output;
//    }
//
//    Complex[] shortToComplex(short[] shorts) {
//        int size = shorts.length;
//        Complex[] complexArray = new Complex[size];
//        float[] floats = shortToFloat(shorts);
//        for (int i = 0; i < size; i++) {
//            complexArray[i] = new Complex((double) floats[i], 0);
//        }
//        return complexArray;
//    }
//
//    float[] complexToFloat(Complex[] complexArray) {
//        int size = complexArray.length;
//        float[] floats = new float[size];
//        for (int i = 0; i < size; i++) {
//            floats[i] = (float) complexArray[i].re();
//        }
//        return floats;
//    }
//
//    short[] cutOff(short[] shorts, int size) {
//        short[] output = new short[size];
//        for (int i = 0; i < size; i++)
//            output[i] = shorts[i];
//        return output;
//    }
//
//    float[] cutOff(float[] shorts, int size) {
//        float[] output = new float[size];
//        for (int i = 0; i < size; i++)
//            output[i] = shorts[i];
//        return output;
//    }
//
//
//    short[] floatToShort(float[] floats) {
//        int size = floats.length;
//        short[] shorts = new short[size];
//        final double factor = 32767;
//        for (int i = 0; i < size; i++) {
//            shorts[i] = (short) (floats[i] * factor);
////            Log.i(TAG, "floatToShort: convert float = " + floats[i]
////                    + " short = " + shorts[i]);
//        }
//        return shorts;
//    }

}

//
//
//public class MainActivity extends AppCompatActivity {
//
//    // Used to load the 'native-lib' library on application startup.
//    static {
//        System.loadLibrary("native-lib");
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
//    }
//
//    /**
//     * A native method that is implemented by the 'native-lib' native library,
//     * which is packaged with this application.
//     */
//    public native String stringFromJNI();
//}
