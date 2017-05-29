package me.demetoir.a3dsound_ndk;

import android.content.pm.ActivityInfo;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

    public static final int ANGLE_COMPENSATOR_VAL = 50;
    private final static int TOTAL_ANGLE_STEP = 20;
    private final static int SAMPLING_RATE = 44100;
    private final static double MAX_DISTANCE = 10;
    private final static int BUFFER_SIZE = 2048;
    private final static int MAX_ANGLE_INDEX_SIZE = 100;
    private final static int HTRF_SIZE = 200;
    private final static int MAX_SOHANDLE_SIZE = 10;

    private double mAngle;
    private double mDistance;
    private float mSOXcor;
    private float mSOYcor;
    private float mOldXcor;
    private float mOldYcor;
    private float mHeadXcor;
    private float mHeadYcor;

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

    // TEXTView
    private TextView mAngleTextView;

    private TextView mDistanceTextView;
    private TextView mXcorTextView;
    private TextView mYcorTextView;
    // IMAGEView
    private ImageView mSoundSourceimageView;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        initButtons();
        initTextView();

        mSoundSourceimageView = (ImageView) findViewById(R.id.sound_source);
        //set image round
        mSoundSourceimageView.setBackground(new ShapeDrawable(new OvalShape()));
        mSoundSourceimageView.setClipToOutline(true);
        mSoundSourceimageView.setOnTouchListener(onTouchListener);


        mSoundArray = loadMonoSound(R.raw.raw_devil);

        initAudioTrack();

        mSOHandleList = new int[MAX_SOHANDLE_SIZE];
        mSoundEngine = new SoundEngine(mAudioTrack);
        mSoundEngine.loadHRTF_database(
                loadHRTFdatabase(R.raw.left_hrtf_database),
                loadHRTFdatabase(R.raw.right_hrtf_database),
                MAX_ANGLE_INDEX_SIZE);

        mSOHandleList[0] = mSoundEngine.makeNewSO(1000, 0, 0, mSoundArray);

    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //        TODO head 이미지 중심 으로 만들고 좌표 구하기
        ImageView headImageView = (ImageView) findViewById(R.id.head);
        int width = headImageView.getRight() - headImageView.getLeft();
        int height = headImageView.getBottom() - headImageView.getTop();
        RelativeLayout parentLayout = (RelativeLayout) headImageView.getParent();
        int parentWidth = parentLayout.getWidth();
        int parentHeight = parentLayout.getHeight();

        headImageView.setX(parentWidth / 2 - width / 2);
        headImageView.setY(parentHeight / 2 - height / 2);
        mHeadXcor = parentWidth / 2;
        mHeadYcor = parentHeight / 2;
        Log.i(TAG, "onWindowFocusChanged: head x : " + mHeadXcor + " y : " + mHeadYcor);

        mSoundSourceimageView.setX(mHeadXcor - mSoundSourceimageView.getWidth() / 2);
        mSoundSourceimageView.setY(mHeadYcor - mSoundSourceimageView.getHeight() / 2);
    }

    private void initTextView() {
        mAngleTextView = (TextView) findViewById(R.id.angleText);
        mAngle = 50;
        updateAngleTextView();

        mDistanceTextView = (TextView) findViewById(R.id.distanceTextView);
        mDistance = 15;
        updateDistanceTextView();

        mXcorTextView = (TextView) findViewById(R.id.XcorTextView);
        updateXcorTextView();

        mYcorTextView = (TextView) findViewById(R.id.YcorTextView);
        updateYcorTextView();
    }

    private void initButtons() {
        mPlayBtn = (Button) findViewById(R.id.playButton);
        mPlayBtn.setOnClickListener(mPlayBtnOnClickListener);

        mStopBtn = (Button) findViewById(R.id.stopButton);
        mStopBtn.setOnClickListener(mStopBtnOnClickListener);

    }

    private void updateAngleTextView() {
        mAngleTextView.setText(String.format(getResources().getString(R.string.textAngleFormat),
                mAngle));
    }

    private void updateDistanceTextView() {
        mDistanceTextView.setText(String.format(getResources().getString(R.string.textDistanceFormat), mDistance));
    }

    private void updateXcorTextView() {
        mXcorTextView.setText(String.format(getResources().getString(R.string.textXcorFormat), mSOXcor));
    }

    private void updateYcorTextView() {
        mYcorTextView.setText(String.format(getResources().getString(R.string.textYcorFormat), mSOYcor));

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

    private ImageView.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int width = ((ViewGroup) v.getParent()).getWidth() - v.getWidth();
            int height = ((ViewGroup) v.getParent()).getHeight() - v.getHeight();

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mOldXcor = event.getX();
                mOldYcor = event.getY();
//                Log.i(TAG, "Action Down X" + event.getX() + "," + event.getY());
//                Log.i(TAG, "Action Down rX " + event.getRawX() + "," + event.getRawY());
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                v.setX(event.getRawX() - mOldXcor);
                v.setY(event.getRawY() - (mOldYcor + v.getHeight()));
//                  Log.i(TAG, "Action Down " + me.getRawX() + "," + me.getRawY());
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (v.getX() > width && v.getY() > height) {
                    v.setX(width);
                    v.setY(height);
                } else if (v.getX() < 0 && v.getY() > height) {
                    v.setX(0);
                    v.setY(height);
                } else if (v.getX() > width && v.getY() < 0) {
                    v.setX(width);
                    v.setY(0);
                } else if (v.getX() < 0 && v.getY() < 0) {
                    v.setX(0);
                    v.setY(0);
                } else if (v.getX() < 0 || v.getX() > width) {
                    if (v.getX() < 0) {
                        v.setX(0);
                        v.setY(event.getRawY() - mOldYcor - v.getHeight());
                    } else {
                        v.setX(width);
                        v.setY(event.getRawY() - mOldYcor - v.getHeight());
                    }
                } else if (v.getY() < 0 || v.getY() > height) {
                    if (v.getY() < 0) {
                        v.setX(event.getRawX() - mOldXcor);
                        v.setY(0);
                    } else {
                        v.setX(event.getRawX() - mOldXcor);
                        v.setY(height);
                    }
                }
            }

            mSOXcor = v.getX() - (mHeadXcor) + v.getWidth() / 2;
            mSOYcor = -(v.getY() - (mHeadYcor)) - v.getHeight() / 2;
            updateXcorTextView();
            updateYcorTextView();

            mSoundEngine.setSOX(0, mSOXcor);
            mSoundEngine.setSOY(0, mSOYcor);


            mAngle = mSoundEngine.getSOAngle(0);
            mDistance = mSoundEngine.getSODistance(0);
            updateAngleTextView();
            updateDistanceTextView();

            return true;
        }
    };

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
