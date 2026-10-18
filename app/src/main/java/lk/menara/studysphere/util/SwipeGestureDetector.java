package lk.menara.studysphere.util;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {

    public interface GestureCallback {
        void onSwipeLeft();
        void onSwipeRight();
        void onSwipeUp();
        void onSwipeDown();
        void onDoubleTap();
        void onLongPress();
    }

    private static final int SWIPE_DISTANCE_DP = 60;
    private static final int SWIPE_VELOCITY_DP = 100;

    private final GestureDetector gestureDetector;
    private final GestureCallback callback;
    private final float swipeDistancePx;
    private final float swipeVelocityPx;

    public SwipeGestureDetector(Context context, GestureCallback callback) {
        this.callback = callback;
        float density = context.getResources().getDisplayMetrics().density;
        this.swipeDistancePx = SWIPE_DISTANCE_DP * density;
        this.swipeVelocityPx = SWIPE_VELOCITY_DP * density;
        this.gestureDetector = new GestureDetector(context, this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null || callback == null) return false;

        float deltaX = e2.getX() - e1.getX();
        float deltaY = e2.getY() - e1.getY();

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            if (Math.abs(deltaX) > swipeDistancePx && Math.abs(velocityX) > swipeVelocityPx) {
                if (deltaX < 0) {
                    callback.onSwipeLeft();
                } else {
                    callback.onSwipeRight();
                }
                return true;
            }
        } else {
            if (Math.abs(deltaY) > swipeDistancePx && Math.abs(velocityY) > swipeVelocityPx) {
                if (deltaY < 0) {
                    callback.onSwipeUp();
                } else {
                    callback.onSwipeDown();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (callback != null) callback.onDoubleTap();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (callback != null) callback.onLongPress();
    }
}