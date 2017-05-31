package me.demetoir.a3dsound_ndk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Yujun-desktop on 2017-05-31.
 */
class OrbitView extends View {

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float initX;
    private float initY;
    private float x;
    private float y;
    private float radius;
    private boolean mIsTouching;

    public OrbitView(Context context) {
        super(context);
        init(context);
    }

    public OrbitView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OrbitView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.CYAN);
        this.initX = 0;
        this.initY = 0;
        this.radius = 0;
        mIsTouching = false;
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
        canvas.drawCircle(x, y, radius, paint);
    }


    public void setX(float x) {
        this.x = x;
    }

    public float GetX() {
        return this.x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getY() {
        return y;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float getRadius() {
        return this.radius;
    }

    public void setIsTouching(boolean flag){
        mIsTouching = flag;
    }

    public boolean getIsTouching(){
        return mIsTouching;
    }
}
