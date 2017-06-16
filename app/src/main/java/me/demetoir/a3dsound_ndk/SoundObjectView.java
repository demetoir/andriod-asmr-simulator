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

    private final static int DEFAULT_SO_HANDEL = 0;
    private final static float CENTER_CIRCLE_RADIUS = 15;
    private final static float CIRCLE_TOUCH_RADIUS = 64;

    public final static int MODE_NONE = 0;
    public final static int MODE_CIRCLE = 1;
    public final static int MODE_LINE = 2;

    public final static int TOUCHING_NONE = 0;
    public final static int TOUCHING_SOUND_OBJECT = 1;
    public final static int TOUCHING_CENTER_POINT = 2;
    public final static int TOUCHING_LINE_START_POINT = 3;
    public final static int TOUCHING_LINE_END_POINT = 4;

    private Paint mOrbitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mCenterPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mLineStartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mLineEndPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Point2D mScreenCenterPoint;
    private Point2D mCenterP;
    private Point2D mLineStartP;
    private Point2D mLineEndP;
    private Point2D mSOPoint;


    private float mRadius;
    private boolean mIsTouching;
    private int mOrbitMode;

    private int mTouchingObject;

    private SoundEngine mSoundEngine;
    private Rect mSoundObjectRect;
    private Rect mStartPointTouchRect;
    private Rect mEndPointTouchRect;
    private Rect mCenterPointTouchRect;

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
        // set paint
        initPaint();

        this.mRadius = 0;
        mScreenCenterPoint = new Point2D();
        mCenterP = new Point2D();
        mLineStartP = new Point2D();
        mLineEndP = new Point2D();
        mSOPoint = new Point2D();

        mIsTouching = false;
        mSOHandle = DEFAULT_SO_HANDEL;
        mOrbitMode = MODE_NONE;

        BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(R.drawable.flat_cycles_32x32);
        mSoundObjectBitmap = bd.getBitmap();

        mSoundObjectRect = new Rect(0, 0, 0, 0);
        mCenterPointTouchRect = new Rect(0, 0, 0, 0);
        mEndPointTouchRect = new Rect(0, 0, 0, 0);
        mStartPointTouchRect = new Rect(0, 0, 0, 0);

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

        if (mOrbitMode == MODE_CIRCLE) {
            mSoundEngine.getSOCenterPoint(DEFAULT_SO_HANDEL, mCenterP);
            Point2D centerP = pToScreenP(mCenterP);
            mRadius = mSoundEngine.getSORadius(DEFAULT_SO_HANDEL);
            Log.i(TAG, "onDraw: center x "+ centerP.x+ " y"+ centerP.y);
            // orbit
            canvas.drawCircle(centerP.x, centerP.y, mRadius, mOrbitPaint);

            // center point
            canvas.drawCircle(centerP.x,
                    centerP.y,
                    CENTER_CIRCLE_RADIUS,
                    mCenterPointPaint);
        } else if (mOrbitMode == MODE_LINE) {
            mSoundEngine.getSOStartPoint(DEFAULT_SO_HANDEL, mLineStartP);
            mSoundEngine.getSOEndPoint(DEFAULT_SO_HANDEL, mLineEndP);
            Point2D startP = pToScreenP(mLineStartP);
            Point2D endP = pToScreenP(mLineEndP);

            // orbit line
            canvas.drawLine(startP.x,
                    startP.y,
                    endP.x,
                    endP.y,
                    mOrbitPaint);

            // start, end point
            canvas.drawCircle(startP.x,
                    startP.y,
                    CENTER_CIRCLE_RADIUS,
                    mLineStartPaint);

            canvas.drawCircle(endP.x,
                    endP.y,
                    CENTER_CIRCLE_RADIUS,
                    mLineEndPaint);
        }

        // TODO set image round
        // draw sound object

//        rectLog(mSoundObjectRect);
        canvas.drawBitmap(mSoundObjectBitmap, null, mSoundObjectRect, null);

    }


    private void rectLog(Rect rect) {
        float a = rect.left;
        float b = rect.top;
        float c = rect.right;
        float d = rect.bottom;
        String str = String.format("%f %f %f %f", a, b, c, d);
        Log.i(TAG, "rectLog: " + str);
    }

    private void pointLog(Point2D p) {
        String str = String.format("%f %f", p.x, p.y);
        Log.i(TAG, "pointLog: point " + str);
    }

    public int pointingObject(Point2D p) {
        pointLog(p);
//        rectLog(mCenterPointTouchRect);
        if (isPointInSO(p)) {
            return TOUCHING_SOUND_OBJECT;
        } else if (isPointInCenter(p)) {
            return TOUCHING_CENTER_POINT;
        } else if (isPointInStartPoint(p)) {
            return TOUCHING_LINE_START_POINT;
        } else if (isPointInEndPoint(p)) {
            return TOUCHING_LINE_END_POINT;
        } else {
            return TOUCHING_NONE;
        }

    }


    public void update() {
        int h = mSoundObjectBitmap.getHeight();
        int w = mSoundObjectBitmap.getWidth();

        mSoundEngine.getSOPoint(mSOHandle, mSOPoint);
        setRectByPoint(mSoundObjectRect, mSOPoint, w, h);
//        Log.i(TAG, "update: sop" + mSOPoint.x + " " + mSOPoint.y);

        mOrbitMode = mSoundEngine.getOrbitMode(DEFAULT_SO_HANDEL);
        if (mOrbitMode == MODE_CIRCLE) {
            mSoundEngine.getSOCenterPoint(DEFAULT_SO_HANDEL, mCenterP);

            pointLog(mCenterP);

            setRectByPoint(mCenterPointTouchRect,
                    mCenterP,
                    (int) CIRCLE_TOUCH_RADIUS,
                    (int) CIRCLE_TOUCH_RADIUS);

            rectLog(mCenterPointTouchRect);

        } else if (mOrbitMode == MODE_LINE) {
            mSoundEngine.getSOStartPoint(DEFAULT_SO_HANDEL, mLineStartP);
            setRectByPoint(mStartPointTouchRect,
                    mLineStartP,
                    (int) CIRCLE_TOUCH_RADIUS,
                    (int) CIRCLE_TOUCH_RADIUS);

            mSoundEngine.getSOEndPoint(DEFAULT_SO_HANDEL, mLineEndP);
            setRectByPoint(mEndPointTouchRect,
                    mLineEndP,
                    (int) CIRCLE_TOUCH_RADIUS,
                    (int) CIRCLE_TOUCH_RADIUS);
        }

//        Log.i(TAG, "update: x  " + x + "  " + y);

        invalidate();
    }

    //////////////////////////////////////////////////
    /////////////////////////////////////////////////

    private boolean isPointInSO(Point2D p) {
        Point2D newP = pToScreenP(p);
        return mSoundObjectRect.contains((int) newP.x, (int) newP.y);
    }

    private boolean isPointInStartPoint(Point2D p) {
        Point2D newP = pToScreenP(p);
        return mStartPointTouchRect.contains((int) newP.x, (int) newP.y);
    }

    private boolean isPointInEndPoint(Point2D p) {
        Point2D newP = pToScreenP(p);
        return mEndPointTouchRect.contains((int) newP.x, (int) newP.y);
    }

    private boolean isPointInCenter(Point2D p) {
        Point2D newP = pToScreenP(p);
        return mCenterPointTouchRect.contains((int) newP.x, (int) newP.y);
    }

    private void initPaint() {
        mOrbitPaint.setStyle(Paint.Style.STROKE);
        mOrbitPaint.setStrokeWidth(3);
        mOrbitPaint.setColor(Color.CYAN);

        mCenterPointPaint.setStyle(Paint.Style.FILL);
        mCenterPointPaint.setColor(Color.RED);

        mLineStartPaint.setStyle(Paint.Style.FILL);
        mLineStartPaint.setColor(Color.RED);

        mLineEndPaint.setStyle(Paint.Style.FILL);
        mLineEndPaint.setColor(Color.BLUE);
    }


    //////////////////////////////////////////////////////
    // getter setter
    ///////////////////////////////////////////////////////


    public void setSoundEngine(SoundEngine soundEngine) {
        mSoundEngine = soundEngine;
    }

    public void setTouching(boolean flag) {
        mIsTouching = flag;
    }

    public boolean IsTouching() {
        return mIsTouching;
    }

    public void setTouchingObject(int object) {
        mTouchingObject = object;
    }

    public int getTouchingObject() {
        return mTouchingObject;
    }

    public void setRectByPoint(Rect rect, Point2D p, int w, int h) {
        Point2D newP = pToScreenP(p);
        int left = (int) (newP.x - w / 2);
        int right = (int) (newP.x + w / 2);
        int top = (int) (newP.y - h / 2);
        int bottom = (int) (newP.y + h / 2);
        rect.set(left, top, right, bottom);
    }

    public void setScreenCenterPoint(Point2D p) {
        mScreenCenterPoint.x = p.x;
        mScreenCenterPoint.y = p.y;
    }

    private Point2D pToScreenP(Point2D p) {
        Point2D newP = new Point2D();
        newP.x = p.x + mScreenCenterPoint.x;
        newP.y = -p.y + mScreenCenterPoint.y;

        return newP;
    }
}
