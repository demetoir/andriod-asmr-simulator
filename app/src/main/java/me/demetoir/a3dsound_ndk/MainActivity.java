package me.demetoir.a3dsound_ndk;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    public static final float DEFAULT_VOLUME = 3.0f;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    private final static int SAMPLING_RATE = 44100;
    private final static int MAX_ANGLE_INDEX_SIZE = 100;
    private final static int HTRF_SIZE = 200;
    private final static int MAX_SOHANDLE_SIZE = 10;
    private final static int DEFAULT_SO_HANDLE = 0;

    private double mDistance;
    private float mSOXcor;
    private float mSOYcor;
    private float mHeadXcor;
    private float mHeadYcor;

    private float[] mSoundArray;

    private AudioTrack mAudioTrack;
    private SoundEngine mSoundEngine;
    private int[] mSOHandleList;

    // FloatingActionButton
    private FloatingActionButton mPlayBtn;
    private FloatingActionButton mMenuBtn;

    // IMAGE View
    private SoundObjectView mSoundObjectView;


    public MainActivity() {
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //상태표시줄 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //화면 회전 고정
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        soundSourceCheck();


        mSoundObjectView = new SoundObjectView(this);
        ((FrameLayout) findViewById(R.id.MainActivityFrameLayout)).addView(mSoundObjectView);

        mSoundArray = loadMonoSound(R.raw.raw_devil);

        initAudioTrack();

        mSOHandleList = new int[MAX_SOHANDLE_SIZE];
        mSoundEngine = new SoundEngine(mAudioTrack);
        mSoundEngine.loadHRTF_database(
                loadHRTFdatabase(R.raw.left_hrtf_database),
                loadHRTFdatabase(R.raw.right_hrtf_database),
                MAX_ANGLE_INDEX_SIZE);

        mSoundEngine.setSoundObjectView(DEFAULT_SO_HANDLE, mSoundObjectView);

        mSOHandleList[DEFAULT_SO_HANDLE] = mSoundEngine.makeNewSO(
                1000,
                200,
                0,
                mSoundArray);

        mSoundEngine.setSOX(0, 200);

        mSoundObjectView.setSoundEngine(mSoundEngine);
        mSoundObjectView.setOnTouchListener(onTouchListener);

        mSoundEngine.setSOOrbitView(DEFAULT_SO_HANDLE, mSoundObjectView);

    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

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

        mSoundObjectView.setCenterX(mHeadXcor);
        mSoundObjectView.setCenterY(mHeadYcor);

        mSoundObjectView.update();
        initButtons();

    }

    private void initButtons() {
        mPlayBtn = (FloatingActionButton) findViewById(R.id.floatingActionButton2);
        mPlayBtn.setOnTouchListener(mPlayBtnOnTouchListener);
        mPlayBtn.setImageResource(R.drawable.ic_media_play);

        mPlayBtn.setX(40);
        mPlayBtn.setY(1150);

        mMenuBtn = (FloatingActionButton) findViewById(R.id.floatingActionButton3);
        mMenuBtn.setOnTouchListener(mMenuBtnOnTouchListener);
        mMenuBtn.setX(600);
        mMenuBtn.setY(40);

//        mStopBtn = (Button) findViewById(R.id.stopButton);
//        mStopBtn.setOnTouchListener(mStopBtnOnTouchListener);

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
        FileOutputStream lOutputStream = openFileOutput(target, Context.MODE_PRIVATE);
        InputStream lInputStream = getResources().openRawResource(resID);
        int readByte;
        byte[] buff = new byte[8048];

        while ((readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff, 0, readByte);
        }

        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void initAudioTrack() {
        int minSize = AudioTrack.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT,
                minSize,
                AudioTrack.MODE_STREAM);

        mAudioTrack.setVolume(DEFAULT_VOLUME);
    }


    private Button.OnTouchListener mPlayBtnOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!mSoundEngine.isPlaying()) {
                    mPlayBtn.setImageResource(R.drawable.ic_media_pause);
                    mSoundEngine.start();
                    mSoundEngine.startSOOrbit(DEFAULT_SO_HANDLE);
                    Log.i(TAG, "onClick: start");
                } else {
                    mPlayBtn.setImageResource(R.drawable.ic_media_play);
                    mSoundEngine.stop();
                    mSoundEngine.stopSOOrbit(DEFAULT_SO_HANDLE);
                    Log.i(TAG, "onClick: stop");
                }
            }
            return false;
        }
    };

    private Button.OnTouchListener mMenuBtnOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Log.i(TAG, "onClick: menu");

            }
            return false;
        }
    };


    private SoundObjectView.OnTouchListener onTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            SoundObjectView SOView = (SoundObjectView) v;
            float eX = event.getX();
            float eY = event.getY();

            Log.i(TAG, "onTouch: w " + SOView.getWidth() + "  h " + SOView.getHeight());
            Log.i(TAG, "onTouch: " + eX + "  " + eY);

            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN && SOView.isXYinSORect(eX, eY)) {
                SOView.setIsTouching(true);
                Log.i(TAG, "onTouch: down");

            } else if (action == MotionEvent.ACTION_MOVE && SOView.getIsTouching()) {
                mSoundEngine.setSOX(DEFAULT_SO_HANDLE, eX - mHeadXcor);
                mSoundEngine.setSOY(DEFAULT_SO_HANDLE, eY - mHeadYcor);
                Log.i(TAG, "onTouch: move");

                SOView.update();

            } else if (action == MotionEvent.ACTION_UP) {
                SOView.setIsTouching(false);
                Log.i(TAG, "onTouch: up");
            } else {
                return false;
            }

            return true;
        }
    };


//    private ImageView.OnTouchListener onTouchListener = new View.OnTouchListener() {
//        @Override
//        public boolean onTouch(View v, MotionEvent event) {
//            int width = ((ViewGroup) v.getParent()).getWidth() - v.getWidth();
//            int height = ((ViewGroup) v.getParent()).getHeight() - v.getHeight();
//            float vX = v.getX();
//            float vY = v.getY();
//            int action = event.getAction();
//
//            if (action == MotionEvent.ACTION_DOWN) {
//                mOldXcor = event.getX();
//                mOldYcor = event.getY();
//            } else if (action == MotionEvent.ACTION_MOVE) {
//                v.setX(event.getRawX() - mOldXcor);
//                v.setY(event.getRawY() - mOldYcor);
//
//            } else if (action == MotionEvent.ACTION_UP) {
//                if (vX > width && vY > height) {
//                    v.setX(width);
//                    v.setY(height);
//                } else if (vX < 0 && vY > height) {
//                    v.setX(0);
//                    v.setY(height);
//                } else if (vX > width && vY < 0) {
//                    v.setX(width);
//                    v.setY(0);
//                } else if (vX < 0 && vY < 0) {
//                    v.setX(0);
//                    v.setY(0);
//                } else if (vX < 0 || vX > width) {
//                    if (vX< 0) {
//                        v.setX(0);
//                        v.setY(event.getRawY() - mOldYcor - v.getHeight());
//                    } else {
//                        v.setX(width);
//                        v.setY(event.getRawY() - mOldYcor - v.getHeight());
//                    }
//                } else if (vY < 0 || vY > height) {
//                    if (vY < 0) {
//                        v.setX(event.getRawX() - mOldXcor);
//                        v.setY(0);
//                    } else {
//                        v.setX(event.getRawX() - mOldXcor);
//                        v.setY(height);
//                    }
//                }
//            }
//
//            mSOXcor = vX - (mHeadXcor) + v.getWidth() / 2;
//            mSOYcor = -(vY - (mHeadYcor)) - v.getHeight() / 2;
//            updateXcorTextView();
//            updateYcorTextView();
//
//            mSoundEngine.setSOX(DEFAULT_SO_HANDLE, mSOXcor);
//            mSoundEngine.setSOY(DEFAULT_SO_HANDLE, mSOYcor);
//
//            mAngle = mSoundEngine.getSOAngle(DEFAULT_SO_HANDLE);
//            mDistance = mSoundEngine.getSODistance(DEFAULT_SO_HANDLE);
//            updateAngleTextView();
//            updateDistanceTextView();
//
//            mSoundObjectView.setRadius((float) mDistance);
//            mSoundObjectView.invalidate();
//            return true;
//        }
//    };

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
