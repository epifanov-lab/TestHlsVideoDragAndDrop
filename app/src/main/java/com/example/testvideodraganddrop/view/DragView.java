package com.example.testvideodraganddrop.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.testvideodraganddrop.utils.GestureDetector;

/**
 * @author Konstantin Epifanov
 * @since 19.08.2019
 */
public class DragView extends View implements GestureDetector.OnGestureListener {

  GestureDetector mGestureDetector;

  public DragView(Context context) {
    this(context, null);
  }

  public DragView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DragView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    mGestureDetector = new GestureDetector(getContext(), this, false);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean result = mGestureDetector.onTouchEvent(event);
    System.out.println("DragView.onTouchEvent " + result);
    return result;
  }

  @Override
  public boolean onDown(MotionEvent event) {
    System.out.println("DragView.onDown");
    return true;
  }

  @Override
  public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
    System.out.println("DragView.onScroll distX:" + distanceX + "  distY:" + distanceY + " ev2:x" + event2.getRawX() + " ev2:y" + event2.getRawY());
    //setX(getX() + (distanceX * -1f));
    //setY(getY() + (distanceY * -1f));
    //setX(event2.getRawX() - getRight() / 2f);
    //setY(event2.getRawY() - getBottom());
    return true;
  }

  /* GESTURES */

  /*
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean result = mGestureDetector.onTouchEvent(event);
    System.out.println("DragView.onTouchEvent " + result);
    return result;
  }

  @Override
  public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
    System.out.println("DragView.onFling: speed " + velocityX + " " + velocityY);
    return true;
  }

  @Override
  public boolean onDown(MotionEvent event) {
    System.out.println("DragView.onDown");
    return true;
  }

  @Override
  public void onShowPress(MotionEvent event) {
    System.out.println("DragView.onShowPress");
  }

  @Override
  public boolean onSingleTapUp(MotionEvent event) {
    System.out.println("DragView.onSingleTapUp");
    return false;
  }

  @Override
  public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
    System.out.println("DragView.onScroll");
    return false;
  }

  @Override
  public void onLongPress(MotionEvent event) {
    System.out.println("DragView.onLongPress");
  }

  @Override
  public boolean onContextClick(MotionEvent event) {
    System.out.println("DragView.onContextClick");
    return false;
  }

  @Override
  public void onSingleTapConfirmed(MotionEvent event) {
    System.out.println("DragView.onSingleTapConfirmed");
  }

  @Override
  public boolean onDoubleTap(MotionEvent event) {
    System.out.println("DragView.onDoubleTap");
    return false;
  }

  @Override
  public boolean onDoubleTapEvent(MotionEvent event) {
    System.out.println("DragView.onDoubleTapEvent");
    return false;
  }
  */

}
