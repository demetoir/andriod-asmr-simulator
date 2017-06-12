package me.demetoir.a3dsound_ndk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

class SoundObjectView extends View {
    private final static String TAG = "SoundObjectView";

    private float CENTER_CIRCLE_RADIUS = 15;
    private Paint orbitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float centerX;
    private float centerY;
    private float radius;
    private boolean mIsTouching;

    private SoundEngine mSoundEngine;

    private Rect mSoundObejectRect;
    private Bitmap mSoundObjectBitmap;
    private int mSOHandle;

    public SoundObjectView(Context context) {
        super(context);
        init(context);
    }

    public SoundObjectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SoundObjectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        orbitPaint.setStyle(Paint.Style.STROKE);
        orbitPaint.setStrokeWidth(3);
        orbitPaint.setColor(Color.CYAN);

        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(Color.RED);

        this.radius = 0;
        centerX = 0;
        centerY = 0;
        mIsTouching = false;
        mSOHandle = 0;

        BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(R.drawable.flat_cycles_32x32);
        mSoundObjectBitmap = bd.getBitmap();

        mSoundObejectRect = new Rect(0, 0, 0, 0);
        Log.i(TAG, "init: ");

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.getMeasuredHeight();
        this.getMeasuredWidth();

        // orbit
        canvas.drawCircle(centerX, centerY, radius, orbitPaint);

        // center point
        canvas.drawCircle(centerX, centerY, CENTER_CIRCLE_RADIUS, centerPaint);


        // TODO set image round
        // draw sound object
        canvas.drawBitmap(mSoundObjectBitmap, null, mSoundObejectRect, null);

    }

    public void setSoundEngine(SoundEngine soundEngine) {
        mSoundEngine = soundEngine;
    }


    public void setCenterX(float x) {
        this.centerX = x;
    }

    public float getCenterX() {
        return this.centerX;
    }

    public void setCenterY(float y) {
        this.centerY = y;
    }

    public float getsCenterY() {
        return centerY;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float getRadius() {
        return this.radius;
    }

    public void setIsTouching(boolean flag) {
        mIsTouching = flag;
    }

    public boolean getIsTouching() {
        return mIsTouching;
    }

    public boolean isXYinSORect(float x, float y) {
        return mSoundObejectRect.contains((int) x, (int) y);
    }

    public boolean isXYInCenterCicle(float x, float y) {
        return centerX - CENTER_CIRCLE_RADIUS <= x
                && x <= centerX + CENTER_CIRCLE_RADIUS
                && centerY - CENTER_CIRCLE_RADIUS <= y
                && y <= centerY + CENTER_CIRCLE_RADIUS;
    }

    public void update() {
        int h = mSoundObjectBitmap.getHeight();
        int w = mSoundObjectBitmap.getWidth();
        int x = (int) mSoundEngine.getSOX(mSOHandle);
        int y = (int) mSoundEngine.getSOY(mSOHandle);
        int left = (int) (x - w / 2 + centerX);
        int right = (int) (x + w / 2 + centerX);
        int top = (int) (y - h / 2 + centerY);
        int bottom = (int) (y + h / 2 + centerY);
        Log.i(TAG, "update: x  " + x + "  " + y);
        mSoundObejectRect.set(left, top, right, bottom);

        float r = mSoundEngine.getSODistance(mSOHandle);
        setRadius(r);
        invalidate();
    }
}
