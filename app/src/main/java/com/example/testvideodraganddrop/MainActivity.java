package com.example.testvideodraganddrop;

import android.os.Bundle;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

  //ViewDragHelper mDragHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.test_2);
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();

    /*findViewById(R.id.drag_view).setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        return false;
      }
    });*/

    /*
    mDragHelper = ViewDragHelper.create(findViewById(R.id.container), 1.0f,
      new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
          System.out.println("DragView.TRY_CAPTURE_VIEW: 1:" + child.getId() + " 2:" + R.id.drag_view);
          return child.getId() == R.id.drag_view;
        }

        @Override
        public void onViewDragStateChanged(int state) {
          *//*
            #STATE_IDLE
            #STATE_DRAGGING
            #STATE_SETTLING
          * *//*
          System.out.println("DragView.STATE " + state);
          super.onViewDragStateChanged(state);
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
          System.out.println("DragView.POSITION_CHANGED " + "left = [" + left + "], top = [" + top + "], dx = [" + dx + "], dy = [" + dy + "]");
          super.onViewPositionChanged(changedView, left, top, dx, dy);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
          System.out.println("DragView.ON_RELEASE =  xvel = [" + xvel + "], yvel = [" + yvel + "]");
          super.onViewReleased(releasedChild, xvel, yvel);
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
          System.out.println("DragView.clampViewPositionHorizontal left = [" + left + "], dx = [" + dx + "]");
          return super.clampViewPositionHorizontal(child, left, dx);
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
          System.out.println("DragView.clampViewPositionVertical top = [" + top + "], dy = [" + dy + "]");
          return super.clampViewPositionVertical(child, top, dy);
        }
      });
      */
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    //mDragHelper.processTouchEvent(event);
    return super.onTouchEvent(event);
  }
}

// Для снапинга использовать SpringAnimation
// OnDragListener
// ViewDragHelper
// GestureDetector onScroll