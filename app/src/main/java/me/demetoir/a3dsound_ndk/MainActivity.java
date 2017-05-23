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
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";

    private double mAngle;
    private final double MAX_ANGLE = 360;
    private final int TOTAL_ANGLE_STEP = 20;
    private final int SAMPLING_RATE = 44100;
    private final double MAX_DISTANCE = 10;
    private final int BUFFER_SIZE = 2048;

    private float[][] mLeftHRTFdatabase;
    private float[][] mRightHRTFdatabase;

    private TextView mAngleTextView;
    private TextView mDistanceTextView;

    private Button mPlayBtn;
    private Button mStopBtn;
    private Button mCCWBtn;
    private Button mCWBtn;
    private Button mDistIncBtn;
    private Button mDistDecBtn;
    private short[] mSoundArray;

    private double mDistance;

    private AudioTrack mAudioTrack;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void playSound() {
        mAudioTrack.play();

        FloatBuffer buf = FloatBuffer.allocate(BUFFER_SIZE*2);


        int angleIdx = getAngleIndex();
        float[] leftH = mLeftHRTFdatabase[angleIdx];
        float[] rightH = mRightHRTFdatabase[angleIdx];
        float[] fSoundArray = shortToFloat(mSoundArray);

        SoundEngine engine = new SoundEngine(leftH, rightH,
                shortToFloat(mSoundArray), mAudioTrack);
        Log.i(TAG, "playSound:  engine inited and start");

        engine.start();

    }

    private int getAngleIndex() {
        return 50 - (int) ((mAngle * 49) / 90);
    }

    private void testPlaySound() {
        short[] sMixOutput = mixMonoToStereo(mSoundArray, mSoundArray);
        float[] fMixOutput = shortToFloat(sMixOutput);

        Log.i(TAG, "playSound: buff conv end");

        Log.i(TAG, "playSound: output len  = " + fMixOutput.length);
        FloatBuffer buf = FloatBuffer.allocate(10000);


        mAudioTrack.play();
        float[] bufOut = new float[8000];
        for (int loop = 0; loop < fMixOutput.length / 8000; loop++) {

            for (int i = 0; i < 8000; i++)
                buf.put(fMixOutput[i+ loop*8000]);

            buf.flip();
            buf.get(bufOut);
            buf.compact();
            int len = mAudioTrack.write(bufOut, 0, bufOut.length,
                    AudioTrack.WRITE_BLOCKING);

        }

//        int cursor = fMixOutput.length - len;
//        Log.i(TAG, "onClick: cursor = " + cursor);
//        while (cursor < fMixOutput.length) {
//            len = mAudioTrack.write(fMixOutput, cursor, fMixOutput.length - cursor,
//                    AudioTrack.WRITE_BLOCKING);
////            mAudioTrack.play();
//            cursor += len;
//
//            Log.i(TAG, "onClick: cursor = " + cursor);
//        }
//        Log.i(TAG, "playSound: len = " + len);

    }

    private Button.OnClickListener mPlayBtnOnClickListener = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onClick(View v) {
//            testPlaySound();
            playSound();
            Log.i(TAG, "onClick: end convolutioin");
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
            mAudioTrack.stop();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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

        mAngleTextView = (TextView) findViewById(R.id.angleText);
        mAngle = 0;
        updateAngleTextView();

        mDistanceTextView = (TextView) findViewById(R.id.distanceTextView);
        mDistance = 0.0;
        updateDistanceTextView();

        mLeftHRTFdatabase = loadHRTFdatabase(R.raw.left_hrtf_database);
        mRightHRTFdatabase = loadHRTFdatabase(R.raw.right_hrtf_database);

        mSoundArray = loadMonoSound(R.raw.raw_explosion);

        initAudioTrack();
    }


    float[][] loadHRTFdatabase(int resId_HRTFDatabase) {
        final int MAX_INDEX = 200;
        float[][] HRTFdatabase = new float[100][200];

        InputStream inputStream = this.getResources().openRawResource(resId_HRTFDatabase);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        try {
            for (int index = 0; index < MAX_INDEX; index++) {
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

    void updateAngleTextView() {
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

    short[] loadMonoSound(int monoSoundResId) {
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
        return monoSound;
    }


    short[] mixMonoToStereo(short[] leftMonoSound, short[] rightMonoSound) {
        int size = (leftMonoSound.length > rightMonoSound.length)
                ? leftMonoSound.length : rightMonoSound.length;
        short[] output = new short[size * 2];
        for (int i = 0; i < size * 2; i++)
            output[i] = 0;

        //left
        for (int i = 0; i < leftMonoSound.length; i++)
            output[i * 2] = leftMonoSound[i];

        //right
        for (int i = 0; i < rightMonoSound.length; i++)
            output[i * 2 + 1] = rightMonoSound[i];

        return output;
    }

    float[] mixMonoToStereo(float[] leftMonoSound, float[] rightMonoSound) {
        int size = (leftMonoSound.length > rightMonoSound.length)
                ? leftMonoSound.length : rightMonoSound.length;
        float[] output = new float[size * 2];
        for (int i = 0; i < size * 2; i++)
            output[i] = 0;

        //left
        for (int i = 0; i < leftMonoSound.length; i++)
            output[i * 2] = leftMonoSound[i];

        //right
        for (int i = 0; i < rightMonoSound.length; i++)
            output[i * 2 + 1] = rightMonoSound[i];

        return output;
    }


    float[] filteringMonoSound(Complex[] monoSound, Complex[] HRTFdatabse) {
        int size = 1;
        while (true) {
            if (size > monoSound.length + HRTFdatabse.length)
                break;
            size *= 2;
        }
        Log.i(TAG, "filteringMonoSound: convolution size = " + size);
        float[] outputSound;

        //convolution
        Complex[] a = new Complex[size];
        Complex[] b = new Complex[size];
        for (int i = 0; i < size; i++) {
            if (i < monoSound.length)
                a[i] = monoSound[i];
            else
                a[i] = new Complex(0, 0);

            if (i < HRTFdatabse.length)
                b[i] = HRTFdatabse[i];
            else
                b[i] = new Complex(0, 0);
        }
        Log.i(TAG, "filteringMonoSound: zero padding end");

        Complex[] convResult = FFT.cconvolve(a, b);
//        for (int i = 0; i < 512; i++) {
//            Log.i(TAG, "filteringMonoSound: conv result i = " + i + " re = " + convResult[i].re()
//                    + " im = " + convResult[i].im());
//        }

        Log.i(TAG, "filteringMonoSound: end convolution");
        //convert Complex to short
        outputSound = complexToFloat(convResult);

        return outputSound;
    }

    short[] setVolumeBy(short[] monoSound, double distance) {
        int size = monoSound.length;
        short[] outputSound = new short[size];
        for (int i = 0; i < size; i++) {
            outputSound[i] = (short) ((double) monoSound[i] * (distance / MAX_DISTANCE));
        }

        return outputSound;
    }

    short[] volumeUp(short[] shorts, double gain) {
        int size = shorts.length;
        short[] output = new short[size];
        for (int i = 0; i < size; i++) {
            double val = (double) shorts[i] * gain;
            output[i] = (short) val;
        }
        return output;
    }

    Complex[] shortToComplex(short[] shorts) {
        int size = shorts.length;
        Complex[] complexArray = new Complex[size];
        float[] floats = shortToFloat(shorts);
        for (int i = 0; i < size; i++) {
            complexArray[i] = new Complex((double) floats[i], 0);
        }
        return complexArray;
    }

    float[] complexToFloat(Complex[] complexArray) {
        int size = complexArray.length;
        float[] floats = new float[size];
        for (int i = 0; i < size; i++) {
            floats[i] = (float) complexArray[i].re();
        }
        return floats;
    }

    short[] cutOff(short[] shorts, int size) {
        short[] output = new short[size];
        for (int i = 0; i < size; i++)
            output[i] = shorts[i];
        return output;
    }

    float[] cutOff(float[] shorts, int size) {
        float[] output = new float[size];
        for (int i = 0; i < size; i++)
            output[i] = shorts[i];
        return output;
    }


    short[] floatToShort(float[] floats) {
        int size = floats.length;
        short[] shorts = new short[size];
        final double factor = 32767;
        for (int i = 0; i < size; i++) {
            shorts[i] = (short) (floats[i] * factor);
//            Log.i(TAG, "floatToShort: convert float = " + floats[i]
//                    + " short = " + shorts[i]);
        }
        return shorts;
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
