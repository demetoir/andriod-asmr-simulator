package me.demetoir.a3dsound_ndk;

import android.content.Intent;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import me.demetoir.a3dsound_ndk.SoundEngine.HRTFDatabase;
import me.demetoir.a3dsound_ndk.SoundEngine.SoundBank;
import me.demetoir.a3dsound_ndk.SoundEngine.SoundEngine;
import me.demetoir.a3dsound_ndk.SoundEngine.SoundObjectView;
import me.demetoir.a3dsound_ndk.SoundEngine.SoundOrbit;
import me.demetoir.a3dsound_ndk.util.Point2D;
import me.demetoir.a3dsound_ndk.util.Util;

import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private final static int MAX_SOHANDLE_SIZE = 10;

    private Point2D mHeadCenterPoint;

    private float[] mSoundArray;

    private AudioTrack mAudioTrack;
    private SoundEngine mSoundEngine;
    private int[] mSOHandleList;

    // FloatingActionButton
    private FloatingActionButton mPlayStopFAB;
    private FloatingActionButton mCloseSettingFAB;

    private SeekBar mSelectSpeedSeekBar;
    private Switch mSelectDirectionSwitch;
    private Switch mSelectSpeedRandomizeSwitch;

    // IMAGE View
    private SoundObjectView mSOView;

    private boolean isCreated;


    public MainActivity() {
        mHeadCenterPoint = new Point2D();
        isCreated = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isCreated) return;
        isCreated = false;

        //상태표시줄 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //화면 회전 고정
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "onCreate: sdfsdfsdf");

        // add mSOView to layer
        mSOView = new SoundObjectView(this);
        ((FrameLayout) findViewById(R.id.MainActivityFrameLayout)).addView(mSOView);

        // load sound
        // TODO 음원 로딩 기능...
        SoundBank soundBank = new SoundBank(this);
        mSoundArray = soundBank.getMonoSound(SoundBank.DEFAULT_SOUND);

        // init AudioTrack
        initAudioTrack();

        // init SoundEngine
        initSoundEngine();

        // init sound object view
        mSOView.setSoundEngine(mSoundEngine);
        mSOView.setOnTouchListener(onTouchListenerSOView);
        mSoundEngine.setSOOrbitView(SoundEngine.DEFAULT_SO_HANDLE, mSOView);

        // init fab and setting ui
        initFABs();
        initSettingUi();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
//        return super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + requestCode);
        int position = data.getIntExtra("res", -1);
        Log.i(TAG, "onActivityResult: position " + position);

        if (position != -1) {
            Toast.makeText(this, "loading sound", Toast.LENGTH_SHORT).show();

            SoundBank soundBank = new SoundBank(this);
            int soundResId = soundBank.getSoundResId(position);
            mSoundEngine.changeSound(SoundEngine.DEFAULT_SO_HANDLE, soundResId);

            Log.i(TAG, "onActivityResult: loading sound");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_select_sound:
                Intent intent = new Intent(MainActivity.this, SelectSoundActivity.class);
                startActivityForResult(intent, 0);

                return true;
            case R.id.menu_set_speed:
                float speed = mSoundEngine.getSoundOrbit(SoundEngine.DEFAULT_SO_HANDLE).getSpeed();
                mSelectSpeedSeekBar.setProgress(Util.speedToProgress(speed,
                        mSelectSpeedSeekBar.getMax()));

                cleanUPUI();
                mSelectSpeedSeekBar.setVisibility(View.VISIBLE);
                mSelectSpeedSeekBar.setClickable(true);
                mSelectSpeedRandomizeSwitch.setClickable(true);
                mSelectSpeedRandomizeSwitch.setVisibility(View.VISIBLE);
                mCloseSettingFAB.setClickable(true);
                mCloseSettingFAB.setVisibility(View.VISIBLE);
                return true;
            case R.id.menu_set_direction:
                int direction = mSoundEngine.getSoundOrbit(SoundEngine.DEFAULT_SO_HANDLE).getDirection();
                if (direction == SoundOrbit.DIRECTION_FORWARD)
                    mSelectDirectionSwitch.setChecked(false);
                else
                    mSelectDirectionSwitch.setChecked(true);

                cleanUPUI();
                mSelectDirectionSwitch.setVisibility(View.VISIBLE);
                mSelectDirectionSwitch.setClickable(true);
                mCloseSettingFAB.setClickable(true);
                mCloseSettingFAB.setVisibility(View.VISIBLE);
                return true;
            case R.id.menu_set_orbit:
                rotateOrbitMode();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // TODO hack 필요
        ImageView headImageView = (ImageView) findViewById(R.id.head);
        int width = headImageView.getRight() - headImageView.getLeft();
        int height = headImageView.getBottom() - headImageView.getTop();

        RelativeLayout parentLayout = (RelativeLayout) headImageView.getParent();
        int parentWidth = parentLayout.getWidth();
        int parentHeight = parentLayout.getHeight();

        headImageView.setX(parentWidth / 2 - width / 2);
        headImageView.setY(parentHeight / 2 - height / 2 - 55);

        mHeadCenterPoint.x = parentWidth / 2;
        mHeadCenterPoint.y = parentHeight / 2 - 55;

        mSOView.setScreenCenterPoint(mHeadCenterPoint);

        mSOView.update();
    }

    void cleanUPUI() {
        mSelectSpeedSeekBar.setClickable(false);
        mSelectSpeedSeekBar.setVisibility(View.GONE);

        mCloseSettingFAB.setClickable(false);
        mCloseSettingFAB.setVisibility(View.GONE);

        mSelectDirectionSwitch.setClickable(false);
        mSelectDirectionSwitch.setVisibility(View.GONE);

        mSelectSpeedRandomizeSwitch.setClickable(false);
        mSelectSpeedRandomizeSwitch.setVisibility(View.GONE);
    }

    private void initFABs() {
        mPlayStopFAB = (FloatingActionButton) findViewById(R.id.playStopFAB);
        mPlayStopFAB.setOnTouchListener(mPlayStopFABOnTouchListener);
        mPlayStopFAB.setImageResource(R.drawable.ic_media_play);

        mPlayStopFAB.setX(40);
        mPlayStopFAB.setY(1000);

        mCloseSettingFAB = (FloatingActionButton) findViewById(R.id.closeSettingFAB);
        mCloseSettingFAB.setOnTouchListener(mCloseSettingFABOnTouchListener);
        mCloseSettingFAB.setX(575);
        mCloseSettingFAB.setY(1000);
        mCloseSettingFAB.setClickable(false);
        mCloseSettingFAB.setVisibility(View.GONE);
    }

    private void initSettingUi() {
        // TODO implement
        mSelectSpeedSeekBar = (SeekBar) findViewById(R.id.seekBarSoundObjectSpeed);
        mSelectSpeedSeekBar.setOnSeekBarChangeListener(mSelectSpeedSeekBarOnSeekBarChangeListener);
        mSelectSpeedSeekBar.setY(880);
        mSelectSpeedSeekBar.setClickable(false);
        mSelectSpeedSeekBar.setVisibility(View.GONE);
        mSelectSpeedSeekBar.setMax(200);
        mSelectSpeedSeekBar.setProgress(50);

        mSelectSpeedRandomizeSwitch = (Switch) findViewById(R.id.switchSoundObjectSpeedRandomize);
        mSelectSpeedRandomizeSwitch.setOnCheckedChangeListener(mSelectSpeedRandomizeSwitchOnCheckedChangeListener);
        mSelectSpeedRandomizeSwitch.setX(250);
        mSelectSpeedRandomizeSwitch.setY(1000);
        mSelectSpeedRandomizeSwitch.setClickable(false);
        mSelectSpeedRandomizeSwitch.setVisibility(View.GONE);

        mSelectDirectionSwitch = (Switch) findViewById(R.id.switchSoundObjectDirection);
        mSelectDirectionSwitch.setOnCheckedChangeListener(mSelectDirectionSwitchOnCheckedChangeListener);
        mSelectDirectionSwitch.setX(300);
        mSelectDirectionSwitch.setY(1000);
        mSelectDirectionSwitch.setClickable(false);
        mSelectDirectionSwitch.setVisibility(View.GONE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void initAudioTrack() {
        int minSize = AudioTrack.getMinBufferSize(SoundBank.DEFAULT_SAMPLING_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SoundBank.DEFAULT_SAMPLING_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT,
                minSize,
                AudioTrack.MODE_STREAM);

        mAudioTrack.setVolume(SoundBank.DEFAULT_VOLUME);
    }

    private void initSoundEngine() {
        mSOHandleList = new int[MAX_SOHANDLE_SIZE];
        mSoundEngine = new SoundEngine(mAudioTrack, this);

        HRTFDatabase hrtfDatabase = new HRTFDatabase(this);
        mSoundEngine.loadHRTF_database(
                hrtfDatabase.leftHRTFDatabase(),
                hrtfDatabase.rightHRTFDatabase(),
                HRTFDatabase.MAX_ANGLE_INDEX_SIZE);

        mSoundEngine.setSoundObjectView(SoundEngine.DEFAULT_SO_HANDLE, mSOView);

        Point2D p = new Point2D(200, 0);
        mSOHandleList[SoundEngine.DEFAULT_SO_HANDLE] = mSoundEngine.makeNewSO(p, mSoundArray);
        mSoundEngine.setSOPoint(SoundEngine.DEFAULT_SO_HANDLE, p);

        Point2D centerP = new Point2D();
        mSoundEngine.setSOCenterPoint(SoundEngine.DEFAULT_SO_HANDLE, centerP);

        Point2D startP = new Point2D(200, 0);
        mSoundEngine.setSOStartPoint(SoundEngine.DEFAULT_SO_HANDLE, startP);

        Point2D endP = new Point2D(-200, 0);
        mSoundEngine.setSOEndPoint(SoundEngine.DEFAULT_SO_HANDLE, endP);

        mSoundEngine.setOrbitMode(SoundEngine.DEFAULT_SO_HANDLE, SoundEngine.MODE_DEFAULT);
    }


    private Switch.OnCheckedChangeListener mSelectSpeedRandomizeSwitchOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mSelectSpeedRandomizeSwitchOnCheckedChangeListener_(isChecked);
        }
    };

    private void mSelectSpeedRandomizeSwitchOnCheckedChangeListener_(boolean isChecked) {
        Log.i(TAG, "onTouch: mSelectSpeedRandomizeSwitchOnCheckedChangeListener_");

        SoundOrbit soundOrbit = mSoundEngine.getSoundOrbit(SoundEngine.DEFAULT_SO_HANDLE);
        soundOrbit.setRandomizeSpeed(isChecked);
    }


    private Switch.OnCheckedChangeListener mSelectDirectionSwitchOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mSelectDirectionSwitchOnCheckedChangeListener_(isChecked);
        }
    };

    private void mSelectDirectionSwitchOnCheckedChangeListener_(boolean isChecked) {
        Log.i(TAG, "onTouch: mSelectDirectionSwitchOnCheckedChangeListener");

        SoundOrbit soundOrbit = mSoundEngine.getSoundOrbit(SoundEngine.DEFAULT_SO_HANDLE);
        if (isChecked) {
            soundOrbit.setDirection(SoundOrbit.DIRECTION_REVERSE);
        } else {
            soundOrbit.setDirection(SoundOrbit.DIRECTION_FORWARD);
        }

        // TODO 함수화
        //
        Point2D startP = new Point2D();
        mSoundEngine.getSOStartPoint(SoundEngine.DEFAULT_SO_HANDLE, startP);
        Point2D endP = new Point2D();
        mSoundEngine.getSOEndPoint(SoundEngine.DEFAULT_SO_HANDLE, endP);

        Point2D nStartP = new Point2D(endP);
        Point2D nEndP = new Point2D(startP);
        mSoundEngine.setSOStartPoint(SoundEngine.DEFAULT_SO_HANDLE, nStartP);
        mSoundEngine.setSOEndPoint(SoundEngine.DEFAULT_SO_HANDLE, nEndP);
    }


    private SeekBar.OnSeekBarChangeListener mSelectSpeedSeekBarOnSeekBarChangeListener
            = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mSoundEngine.getSoundOrbit(SoundEngine.DEFAULT_SO_HANDLE)
                    .setSpeed(Util.progressToSpeed(progress, seekBar.getMax()));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.i(TAG, "onStartTrackingTouch: ");
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.i(TAG, "onStopTrackingTouch: ");
        }
    };


    private Button.OnTouchListener mPlayStopFABOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mPlayStopFABOnTouchListener_(event);
        }
    };


    private boolean mPlayStopFABOnTouchListener_(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            updatePlayStopFAB();
        }
        return false;
    }

    void updatePlayStopFAB() {
        if (!mSoundEngine.isPlaying()) {
            mPlayStopFAB.setImageResource(R.drawable.ic_media_pause);
            mSoundEngine.start();
            mSoundEngine.startSOOrbit(SoundEngine.DEFAULT_SO_HANDLE);
            Log.i(TAG, "onClick: start");
        } else {
            mPlayStopFAB.setImageResource(R.drawable.ic_media_play);
            mSoundEngine.stop();
            mSoundEngine.stopSOOrbit(SoundEngine.DEFAULT_SO_HANDLE);
            Log.i(TAG, "onClick: stop");
        }
    }

    private void rotateOrbitMode() {
        int mode = mSoundEngine.getOrbitMode(SoundEngine.DEFAULT_SO_HANDLE);
        //set circle to  line
        if (mode == SoundEngine.MODE_CIRCLE) {
            mSoundEngine.setOrbitMode(SoundEngine.DEFAULT_SO_HANDLE, SoundEngine.MODE_LINE);

            Point2D p = new Point2D();
            mSoundEngine.getSOStartPoint(SoundEngine.DEFAULT_SO_HANDLE, p);
            mSoundEngine.setSOPoint(SoundEngine.DEFAULT_SO_HANDLE, p);
            mSOView.update();
            makeText(this, "line orbit", Toast.LENGTH_SHORT).show();
        }
        // line to random
        else if (mode == SoundEngine.MODE_LINE) {
            mSoundEngine.setOrbitMode(SoundEngine.DEFAULT_SO_HANDLE, SoundEngine.MODE_RANDOM);
            mSOView.update();
            makeText(this, "random orbit", Toast.LENGTH_SHORT).show();

        }
        // random to circle
        else if (mode == SoundEngine.MODE_RANDOM) {
            mSoundEngine.setOrbitMode(SoundEngine.DEFAULT_SO_HANDLE, SoundEngine.MODE_FREE);
            makeText(this, "free orbit", Toast.LENGTH_SHORT).show();

            mSOView.update();
        } else if (mode == SoundEngine.MODE_FREE) {
            mSoundEngine.setOrbitMode(SoundEngine.DEFAULT_SO_HANDLE, SoundEngine.MODE_CIRCLE);
            mSOView.update();
            makeText(this, "circle orbit", Toast.LENGTH_SHORT).show();

        }
    }

    private Button.OnTouchListener mCloseSettingFABOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mCloseSettingFABOnTouchListener_(event);
        }
    };

    private boolean mCloseSettingFABOnTouchListener_(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            cleanUPUI();
        }
        return false;
    }

    private SoundObjectView.OnTouchListener onTouchListenerSOView = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return onTouchListenerSOView_(v, event);
        }
    };

    private boolean onTouchListenerSOView_(View v, MotionEvent event) {
        SoundObjectView SOView = (SoundObjectView) v;
        int orbitMode = mSoundEngine.getOrbitMode(SoundEngine.DEFAULT_SO_HANDLE);
        float eX = event.getX();
        float eY = event.getY();

        // screen point to point
        Point2D newP = new Point2D(eX - mHeadCenterPoint.x, -eY + mHeadCenterPoint.y);

        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            int object = SOView.pointingObject(newP);
            if (object == SoundObjectView.TOUCHING_NONE) {
                return false;
            } else {
                SOView.setTouchingObject(object);
                SOView.setTouching(true);
                return true;
            }
        } else if (action == MotionEvent.ACTION_MOVE && SOView.IsTouching()) {
            int object = SOView.getTouchingObject();

            if (object == SoundObjectView.TOUCHING_SOUND_OBJECT
                    &&  orbitMode != SoundEngine.MODE_RANDOM) {
                Point2D oldP = new Point2D();
                mSoundEngine.getSOPoint(SoundEngine.DEFAULT_SO_HANDLE, oldP);

                if (orbitMode == SoundEngine.MODE_LINE) {
                    Point2D dP = newP.sub(oldP);

                    Point2D oldStartP = new Point2D();
                    mSoundEngine.getSOStartPoint(SoundEngine.DEFAULT_SO_HANDLE, oldStartP);
                    mSoundEngine.setSOStartPoint(SoundEngine.DEFAULT_SO_HANDLE, oldStartP.add(dP));

                    Point2D oldEndP = new Point2D();
                    mSoundEngine.getSOEndPoint(SoundEngine.DEFAULT_SO_HANDLE, oldEndP);
                    mSoundEngine.setSOEndPoint(SoundEngine.DEFAULT_SO_HANDLE, oldEndP.add(dP));
                }

                mSoundEngine.setSOPoint(SoundEngine.DEFAULT_SO_HANDLE, newP);

            } else if (object == SoundObjectView.TOUCHING_CENTER_POINT
                    && orbitMode == SoundEngine.MODE_CIRCLE) {
                mSoundEngine.setSOCenterPoint(SoundEngine.DEFAULT_SO_HANDLE, newP);

            } else if (object == SoundObjectView.TOUCHING_LINE_END_POINT
                    && orbitMode == SoundEngine.MODE_LINE) {

                Point2D oldSOP = new Point2D();
                mSoundEngine.getSOPoint(SoundEngine.DEFAULT_SO_HANDLE, oldSOP);

                Point2D oldEndP = new Point2D();
                mSoundEngine.getSOEndPoint(SoundEngine.DEFAULT_SO_HANDLE, oldEndP);

                Point2D startP = new Point2D();
                mSoundEngine.getSOStartPoint(SoundEngine.DEFAULT_SO_HANDLE, startP);

                double r = startP.distance(oldSOP);

                double angle = startP.angle(newP);
                double dx = r * Math.cos(angle);
                double dy = r * Math.sin(angle);

                Point2D newSOP = startP.sub(dx, dy);
                mSoundEngine.setSOPoint(SoundEngine.DEFAULT_SO_HANDLE, newSOP);

                mSoundEngine.setSOEndPoint(SoundEngine.DEFAULT_SO_HANDLE, newP);

            } else if (object == SoundObjectView.TOUCHING_LINE_START_POINT
                    && orbitMode == SoundEngine.MODE_LINE) {

                Point2D oldSOP = new Point2D();
                mSoundEngine.getSOPoint(SoundEngine.DEFAULT_SO_HANDLE, oldSOP);

                Point2D endP = new Point2D();
                mSoundEngine.getSOEndPoint(SoundEngine.DEFAULT_SO_HANDLE, endP);

                Point2D oldStartP = new Point2D();
                mSoundEngine.getSOStartPoint(SoundEngine.DEFAULT_SO_HANDLE, oldStartP);

                Point2D newStartP = newP;

                double r = oldStartP.distance(oldSOP);
                double angle = newStartP.angle(endP);
                double dx = r * Math.cos(angle);
                double dy = r * Math.sin(angle);

                Point2D newSOP = newStartP.sub(dx, dy);
                mSoundEngine.setSOPoint(SoundEngine.DEFAULT_SO_HANDLE, newSOP);

                mSoundEngine.setSOStartPoint(SoundEngine.DEFAULT_SO_HANDLE, newStartP);
            }

            SOView.update();
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            SOView.setTouching(false);
            SOView.setTouchingObject(SoundObjectView.TOUCHING_NONE);
            return false;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        mSoundEngine.stop();
        super.onBackPressed();
    }

    public SoundEngine getSoundEngine() {
        return mSoundEngine;
    }
}
