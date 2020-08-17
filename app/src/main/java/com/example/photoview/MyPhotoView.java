package com.example.photoview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.Nullable;

/**
 * @author hugoca on 2020/8/14.
 */
public class MyPhotoView extends View {
    private static final float IMAGE_WIDTH=Utils.dpToPixel(300);
    private static final float SCALE_FACTOR=1.5f;
    private Bitmap mBitmap;
    private Paint mPaint;

    //图片起始位置偏移坐标
    private float originalOffsetX;
    private float originalOffsetY;

     private float smallScale; //横向缩放填充
     private float bigScale;  //纵向缩放填充

    private float curScale; //当前缩放
    private boolean isEnLarge; //是否已经放大

    private GestureDetector gestureDetector; //手势操作
    private ObjectAnimator scaleAnimator; //处理缩放的动画
    private OverScroller overScroller; //处理惯性滑动
    private ScaleGestureDetector scaleGestureDetector;

    //滑动偏移
    private float offsetX;
    private float offsetY;
    private float scaleFactor=0.0f; //操作后的缩放因子

    public MyPhotoView(Context context) {
        super(context);
        init(context);
    }

    public MyPhotoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyPhotoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        mBitmap=Utils.getPhoto(getResources(), (int) IMAGE_WIDTH);
        mPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        gestureDetector=new GestureDetector(context, new photoGestureDetector());
        // 关闭长按响应
//        gestureDetector.setIsLongpressEnabled(false);
        overScroller=new OverScroller(context);
        scaleGestureDetector=new ScaleGestureDetector(context,new PhotoScakeGestrueListener());
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        scaleFactor=(curScale-smallScale)/(bigScale-smallScale);
        //当前放大比例为small时，scaleFactor=0 不偏移
        //通过设置所放量来决定bitmap显示的偏移量
        canvas.translate(offsetX*scaleFactor,offsetY*scaleFactor);
        canvas.scale(curScale,curScale,getWidth()/2f,getHeight()/2f);
        canvas.drawBitmap(mBitmap,originalOffsetX,originalOffsetY,mPaint);
    }

    /**
     * 在onDraw之前就会调用
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        originalOffsetX=(getWidth()-mBitmap.getWidth())/2f;
        originalOffsetY=(getHeight()-mBitmap.getHeight())/2f;

        if((float)mBitmap.getWidth()/mBitmap.getHeight()>(float) getWidth()/getHeight()){
            smallScale=(float) getWidth()/mBitmap.getWidth();
            bigScale=(float)getHeight()/mBitmap.getHeight()*SCALE_FACTOR;
        }else {
            smallScale=(float)getHeight()/mBitmap.getHeight();
            bigScale=(float) getWidth()/mBitmap.getWidth()*SCALE_FACTOR;

        }
        curScale=smallScale;
    }



    private ObjectAnimator getScaleAnimation(){
        if(scaleAnimator==null){
            scaleAnimator=ObjectAnimator.ofFloat(this,"curScale",0);
        }
        scaleAnimator.setFloatValues(smallScale,bigScale);
        return scaleAnimator;
    }

    public float getCurScale() {
        return curScale;
    }

    public void setCurScale(float curScale) {
        this.curScale = curScale;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 响应事件以双指缩放优先,当不响应双指操作后再处理其他手势
        boolean result=scaleGestureDetector.onTouchEvent(event);
        if(!scaleGestureDetector.isInProgress()){
            result=gestureDetector.onTouchEvent(event);
        }
        return result;
    }

    class photoGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            isEnLarge=!isEnLarge;
            if(isEnLarge){
                offsetX=0;
                offsetY=0;
                offsetX=(e.getX()-getWidth()/2f)-(e.getX()-getWidth()/2f)*bigScale/smallScale;
                offsetY=(e.getY()-getHeight()/2f)-(e.getY()-getHeight()/2f)*bigScale/smallScale;
                getScaleAnimation().start();
            }else {
                getScaleAnimation().reverse();
            }
            return super.onDoubleTap(e);
        }

        /**
         *
         * @param e1        手指按下
         * @param e2        当前的
         * @param distanceX 旧位置 - 新位置
         * @param distanceY
         * @return
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if(isEnLarge){
                if(distanceX>0){
                    offsetX=Math.max(offsetX-distanceX,-getMaxWidth());
                }else {
                    offsetX=Math.min(offsetX-distanceX,getMaxWidth());
                }
                if(distanceY>0){
                    offsetY=Math.max(offsetY-distanceY,-getMaxHeight());
                }else {
                    offsetY=Math.min(offsetY-distanceY,getMaxHeight());
                }
                invalidate();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override //惯性滑动
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if(isEnLarge){
                overScroller.fling((int)offsetX,(int)offsetY,
                        (int)velocityX,(int)velocityY,
                        -(int)getMaxWidth(),(int)getMaxWidth()
                        ,-(int)getMaxHeight(),(int)getMaxHeight(),300,300);
                postOnAnimation(new FingRunnable());
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    class FingRunnable implements Runnable{

        @Override
        public void run() {
            if (overScroller.computeScrollOffset()) {
                offsetX=overScroller.getCurrX();
                offsetY=overScroller.getCurrY();
                invalidate();
                postOnAnimation(this);
            }
        }
    }

    class PhotoScakeGestrueListener implements ScaleGestureDetector.OnScaleGestureListener{

        private float beginScale;
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            if((curScale>smallScale&&!isEnLarge)||(curScale==smallScale&&!isEnLarge)){
                isEnLarge= true;
            }
            curScale=beginScale*scaleGestureDetector.getScaleFactor(); //缩放因子
            invalidate();
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            beginScale=curScale;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

        }
    }

    private float getMaxWidth(){
        return (mBitmap.getWidth()*bigScale-getWidth())/2;
    }

    private float getMaxHeight(){
        return (mBitmap.getHeight()*bigScale-getHeight())/2;
    }

}
