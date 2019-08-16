package com.example.testvideodraganddrop.utils;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import static android.os.Message.obtain;
import static android.view.MotionEvent.ACTION_BUTTON_PRESS;
import static android.view.MotionEvent.ACTION_BUTTON_RELEASE;
import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MASK;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.MotionEvent.BUTTON_SECONDARY;
import static android.view.MotionEvent.BUTTON_STYLUS_PRIMARY;

/**
 * Detects various gestures and events using the supplied {@link MotionEvent}s.
 * The {@link OnGestureListener} callback will notify users when a particular
 * motion event has occurred. This class should only be used with {@link MotionEvent}s
 * reported via touch (don't use for trackball events).
 *
 * To use this class:
 * <ul>
 *  <li>Create an instance of the {@code GestureDetector} for your {@link View}
 *  <li>In the {@link View#onTouchEvent(MotionEvent)} method ensure you call
 *          {@link #onTouchEvent(MotionEvent)}. The methods defined in your callback
 *          will be executed when the events occur.
 *  <li>If listening for {@link OnGestureListener#onContextClick(MotionEvent)}
 *          you must call {@link #onGenericMotionEvent(MotionEvent)}
 *          in {@link View#onGenericMotionEvent(MotionEvent)}.
 * </ul>
 */
public class GestureDetector {

  private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
  private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
  private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
  private static final int DOUBLE_TAP_MIN_TIME = /*ViewConfiguration.getDoubleTapMinTime()*/40;

  private final int mTouchSlopSquare;
  private final int mDoubleTapTouchSlopSquare;
  private final int mDoubleTapSlopSquare;
  private final int mMinimumFlingVelocity;
  private final int mMaximumFlingVelocity;

  private final Handler mHandler;
  private final OnGestureListener mListener;

  private boolean mStillDown;
  private boolean mDeferConfirmSingleTap;
  private boolean mInLongPress;
  private boolean mInContextClick;
  private boolean mAlwaysInTapRegion;
  private boolean mAlwaysInBiggerTapRegion;
  private boolean mIgnoreNextUpEvent;

  private MotionEvent mCurrentDownEvent;
  private MotionEvent mPreviousUpEvent;

  /**
   * True when the user is still touching for the second tap (down, move, and
   * up events). Can only be true if there is a double tap listener attached.
   */
  private boolean mIsDoubleTapping;

  private float mLastFocusX, mLastFocusY, mDownFocusX, mDownFocusY;
  private final boolean mIsLongpressEnabled;

  /** Determines speed during touch scrolling. */
  private VelocityTracker mVelocityTracker;

  /**
   * Creates a GestureDetector with the supplied listener that runs deferred events on the thread
   * associated with the supplied {@link Looper}.
   *
   * @param context the application's context
   * @param listener the listener invoked for all the callbacks, this must not be null.
   * @param isLongpressEnabled whether longpress should be enabled.
   */
  GestureDetector(Context context, OnGestureListener listener, boolean isLongpressEnabled) {
    mHandler = new Handler(); mListener = listener;
    /*
     * Set whether longpress is enabled, if this is enabled when a user
     * presses and holds down you get a longpress event and nothing further.
     * If it's disabled the user can press and hold down and then later
     * moved their finger and you will get scroll events. By default
     * longpress is enabled.
     */
    mIsLongpressEnabled = isLongpressEnabled;
    final ViewConfiguration configuration = ViewConfiguration.get(context);
    final int touchSlop = configuration.getScaledTouchSlop();
    //noinspection UnnecessaryLocalVariable
    final int doubleTapTouchSlop = touchSlop;
    final int doubleTapSlop = configuration.getScaledDoubleTapSlop();
    mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
    mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
    mTouchSlopSquare = touchSlop * touchSlop;
    mDoubleTapTouchSlopSquare = doubleTapTouchSlop * doubleTapTouchSlop;
    mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
  }

  /**
   * Analyzes the given motion event and if applicable triggers the
   * appropriate callbacks on the {@link OnGestureListener} supplied.
   *
   * @param event The current motion event.
   * @return true if the {@link OnGestureListener} consumed the event, else false.
   */
  @SuppressWarnings("ConstantConditions")
  public boolean onTouchEvent(MotionEvent event) {

    final int action = event.getAction();

    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    mVelocityTracker.addMovement(event);

    final boolean pointerUp = (action & ACTION_MASK) == ACTION_POINTER_UP;
    final int skipIndex = pointerUp ? event.getActionIndex() : -1;

    final boolean isGeneratedGesture = (event.getFlags() & 0x8) != 0;
    //final boolean isGeneratedGesture = true;

    // Determine focal point
    float sumX = 0, sumY = 0;
    final int count = event.getPointerCount();
    for (int i = 0; i < count; i++) {
      if (skipIndex == i) continue;
      sumX += event.getX(i);
      sumY += event.getY(i);
    }
    final int div = pointerUp ? count - 1 : count;
    final float focusX = sumX / div;
    final float focusY = sumY / div;

    boolean handled = false;

    switch (action & ACTION_MASK) {
      case ACTION_POINTER_DOWN:
        mDownFocusX = mLastFocusX = focusX;
        mDownFocusY = mLastFocusY = focusY;
        // Cancel long press and taps
        cancelTaps();
        break;

      case ACTION_POINTER_UP:
        mDownFocusX = mLastFocusX = focusX;
        mDownFocusY = mLastFocusY = focusY;

        // Check the dot product of current velocities.
        // If the pointer that left was opposing another velocity vector, clear.
        mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
        final int upIndex = event.getActionIndex();
        final int id1 = event.getPointerId(upIndex);
        final float x1 = mVelocityTracker.getXVelocity(id1);
        final float y1 = mVelocityTracker.getYVelocity(id1);
        for (int i = 0; i < count; i++) {
          if (i == upIndex) continue;

          final int id2 = event.getPointerId(i);
          final float x = x1 * mVelocityTracker.getXVelocity(id2);
          final float y = y1 * mVelocityTracker.getYVelocity(id2);

          final float dot = x + y;
          if (dot < 0) {
            mVelocityTracker.clear();
            break;
          }
        }
        break;

      case ACTION_DOWN:
        boolean hadTapMessage = mHandler.unscheduleTapPress();
        if ((mCurrentDownEvent != null) && (mPreviousUpEvent != null) && hadTapMessage
          && isConsideredDoubleTap(mCurrentDownEvent, mPreviousUpEvent, event)) {
          // This is a second tap
          mIsDoubleTapping = true;
          // Give a callback with the first tap of the double-tap
          handled |= mListener.onDoubleTap(mCurrentDownEvent);
          // Give a callback with down event of the double-tap
          handled |= mListener.onDoubleTapEvent(event);
        } else
          // This is a first tap
          if(!mHandler.scheduleTapPress(() -> {
            // If the user's finger is still down, do not count it as a tap
            if (mStillDown) mDeferConfirmSingleTap = true;
            else mListener.onSingleTapConfirmed(mCurrentDownEvent);
          }, SystemClock.uptimeMillis() + DOUBLE_TAP_TIMEOUT)) break;

        mDownFocusX = mLastFocusX = focusX;
        mDownFocusY = mLastFocusY = focusY;
        if (mCurrentDownEvent != null) mCurrentDownEvent.recycle();
        mCurrentDownEvent = MotionEvent.obtain(event);
        mAlwaysInTapRegion = true;
        mAlwaysInBiggerTapRegion = true;
        mStillDown = true;
        mInLongPress = false;
        mDeferConfirmSingleTap = false;
        if (mIsLongpressEnabled) {
          mHandler.unscheduleLongPress();
          if(!mHandler.scheduleLongPress(() -> {
            mHandler.unscheduleTapPress();
            mDeferConfirmSingleTap = false;
            mInLongPress = true;
            mListener.onLongPress(mCurrentDownEvent);
          }, mCurrentDownEvent.getDownTime() + LONGPRESS_TIMEOUT)) break;
        }
        if (mHandler.scheduleShowPress
          (() -> mListener.onShowPress(mCurrentDownEvent),
          mCurrentDownEvent.getDownTime() + TAP_TIMEOUT))
          handled |= mListener.onDown(event);
        break;

      case ACTION_MOVE:
        if (mInLongPress || mInContextClick) break;
        final float scrollX = mLastFocusX - focusX;
        final float scrollY = mLastFocusY - focusY;
        if (mIsDoubleTapping)
          // Give the move events of the double-tap
          handled |= mListener.onDoubleTapEvent(event);
        else if (mAlwaysInTapRegion) {
          final int deltaX = (int) (focusX - mDownFocusX);
          final int deltaY = (int) (focusY - mDownFocusY);
          int distance = (deltaX * deltaX) + (deltaY * deltaY);
          int slopSquare = isGeneratedGesture ? 0 : mTouchSlopSquare;
          if (distance > slopSquare) {
            handled = mListener.onScroll(mCurrentDownEvent, event, scrollX, scrollY);
            mLastFocusX = focusX;
            mLastFocusY = focusY;
            mAlwaysInTapRegion = false;
            mHandler.unscheduleAll();
          }
          int doubleTapSlopSquare = isGeneratedGesture ? 0 : mDoubleTapTouchSlopSquare;
          if (distance > doubleTapSlopSquare) mAlwaysInBiggerTapRegion = false;
        } else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
          handled = mListener.onScroll(mCurrentDownEvent, event, scrollX, scrollY);
          mLastFocusX = focusX; mLastFocusY = focusY;
        }
        break;

      case ACTION_UP:
        mStillDown = false;
        MotionEvent currentUpEvent = MotionEvent.obtain(event);
        if (mIsDoubleTapping)
          // Finally, give the up event of the double-tap
          handled |= mListener.onDoubleTapEvent(event);
        else if (mInLongPress) {
          mHandler.unscheduleTapPress();
          mInLongPress = false;
        } else if (mAlwaysInTapRegion && !mIgnoreNextUpEvent) {
          handled = mListener.onSingleTapUp(event);
          if (mDeferConfirmSingleTap)
            mHandler.scheduleTapPress(() -> mListener.onSingleTapConfirmed(event), 0);
        } else if (!mIgnoreNextUpEvent) {
          // A fling must travel the minimum tap distance
          final VelocityTracker velocityTracker = mVelocityTracker;
          final int pointerId = event.getPointerId(0);
          velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
          final float velocityY = velocityTracker.getYVelocity(pointerId);
          final float velocityX = velocityTracker.getXVelocity(pointerId);

          if ((Math.abs(velocityY) > mMinimumFlingVelocity) ||
            (Math.abs(velocityX) > mMinimumFlingVelocity))
            handled = mListener.onFling(mCurrentDownEvent, event, velocityX, velocityY);
        }
        if (mPreviousUpEvent != null) mPreviousUpEvent.recycle();
        // Hold the event we obtained above - listeners may have changed the original.
        mPreviousUpEvent = currentUpEvent;
        if (mVelocityTracker != null) {
          // This may have been cleared when we called out to the
          // application above.
          mVelocityTracker.recycle();
          mVelocityTracker = null;
        }
        mIsDoubleTapping = false;
        mDeferConfirmSingleTap = false;
        mIgnoreNextUpEvent = false;
        mHandler.unscheduleShowPress();
        mHandler.unscheduleLongPress();
        break;

      case ACTION_CANCEL: cancel(); break;
    }

    return handled;
  }

  /**
   * Analyzes the given generic motion event and if applicable triggers the
   * appropriate callbacks on the {@link OnGestureListener} supplied.
   *
   * @param event The current motion event.
   * @return true if the {@link OnGestureListener} consumed the event, else false.
   */
  public final boolean onGenericMotionEvent(MotionEvent event) {
    final int actionButton = event.getActionButton();
    switch (event.getActionMasked()) {
      case ACTION_BUTTON_PRESS:
        if (!mInContextClick && !mInLongPress
          && (actionButton == BUTTON_STYLUS_PRIMARY || actionButton == BUTTON_SECONDARY))
          if (mListener.onContextClick(event)) {
            mInContextClick = true;
            mHandler.unscheduleLongPress();
            mHandler.unscheduleTapPress();
            return true;
          }
        break;
      case ACTION_BUTTON_RELEASE:
        if (mInContextClick &&
          (actionButton == BUTTON_STYLUS_PRIMARY ||
            actionButton == BUTTON_SECONDARY)) {
          mInContextClick = false;
          mIgnoreNextUpEvent = true;
        }
        break;
    }
    return false;
  }

  /** Cancel action. */
  private void cancel() {
    mHandler.unscheduleAll();
    mVelocityTracker.recycle();
    mVelocityTracker = null;
    mIsDoubleTapping = false;
    mStillDown = false;
    mAlwaysInTapRegion = false;
    mAlwaysInBiggerTapRegion = false;
    mDeferConfirmSingleTap = false;
    mInLongPress = false;
    mInContextClick = false;
    mIgnoreNextUpEvent = false;
  }

  /** Cancel taps. */
  private void cancelTaps() {
    mHandler.unscheduleAll();
    mIsDoubleTapping = false;
    mAlwaysInTapRegion = false;
    mAlwaysInBiggerTapRegion = false;
    mDeferConfirmSingleTap = false;
    mInLongPress = false;
    mInContextClick = false;
    mIgnoreNextUpEvent = false;
  }

  /**
   * @param firstDown first down action
   * @param firstUp   first up action
   * @param secondDown second down action
   *
   * @return true if considered
   */
  private boolean isConsideredDoubleTap
    (MotionEvent firstDown, MotionEvent firstUp, MotionEvent secondDown) {
    if (!mAlwaysInBiggerTapRegion) return false;
    final long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();
    if (deltaTime > DOUBLE_TAP_TIMEOUT || deltaTime < DOUBLE_TAP_MIN_TIME) return false;
    int deltaX = (int) firstDown.getX() - (int) secondDown.getX();
    int deltaY = (int) firstDown.getY() - (int) secondDown.getY();
    final boolean isGeneratedGesture =
      (firstDown.getFlags() & /*MotionEvent.FLAG_IS_GENERATED_GESTURE*/0x8) != 0;
    int slopSquare = isGeneratedGesture ? 0 : mDoubleTapSlopSquare;
    return (deltaX * deltaX + deltaY * deltaY < slopSquare);
  }


  /** Internal Handler */
  private static final class Handler {

    /** Constants for Message.what used by GestureHandler below. */
    private static final int SHOW_PRESS = 100, LONG_PRESS = 101, TAP_PRESS = 102;


    /** Handler. */
    private final android.os.Handler mHandler;

    /** Constructs a new {@link Handler}. */
    Handler()
    {mHandler = new android.os.Handler(new Callback());}

    /**
     * Schedule show press action.
     *
     * @param task action task
     * @param time schedule time
     *
     * @return success
     */
    final boolean scheduleShowPress
    (Runnable task, long time)
    {return schedule(SHOW_PRESS, task, time);}

    /** Unschedule show press. */
    final void unscheduleShowPress()
    {mHandler.removeMessages(SHOW_PRESS);}

    /**
     * Schedule long press action.
     *
     * @param task action task
     * @param time schedule time
     *
     * @return success
     */
    final boolean scheduleLongPress
    (Runnable task, long time) {
      //System.out.println("Handler.scheduleLongPress");
      return schedule(LONG_PRESS, task, time);
    }

    /** Unschedule long press. */
    final void unscheduleLongPress()
    {mHandler.removeMessages(LONG_PRESS);}

    /**
     * Schedule tap press action.
     *
     * @param task action task
     * @param time schedule time
     *
     * @return success
     */
    final boolean scheduleTapPress
    (Runnable task, long time)
    {return schedule(TAP_PRESS, task, time);}

    /** Unschedule tap press. */
    final boolean unscheduleTapPress() {
      final boolean result = mHandler.hasMessages(TAP_PRESS);
      if (result) mHandler.removeMessages(TAP_PRESS); return result;
    }

    /**
     * Schedule action.
     *
     * @param action action id
     * @param task action task
     * @param time schedule time
     *
     * @return success
     */
    private boolean schedule(int action, Runnable task, long time) {
      if (time == 0) {
        task.run();
        return true;
      } else {
        final Message message = obtain(mHandler, action, task);
        message.setAsynchronous(true);
        return mHandler.sendMessageAtTime(message, time);
      }
    }

    /** Unschedule all commands. */
    final void unscheduleAll() {
      unscheduleShowPress();
      unscheduleLongPress();
      unscheduleTapPress();
    }

    /** Handler's Callback */
    private static final class Callback implements android.os.Handler.Callback {

      /** {@inheritDoc} */
      @Override public final boolean handleMessage(Message message) {
        switch (message.what) {
          case SHOW_PRESS: case LONG_PRESS: case TAP_PRESS:
            final Runnable runnable = (Runnable) message.obj;
            message.obj = null; runnable.run(); return true;
          default: return false;
        }
      }
    }
  }


  /**
   * The listener that is used to notify when gestures occur.
   * If you want to listen for all the different gestures then implement
   * this interface.
   */
  @SuppressWarnings({ "unused", "SameReturnValue" })
  interface OnGestureListener {

    /**
     * Notified when a tap occurs with the down {@link MotionEvent}
     * that triggered it. This will be triggered immediately for
     * every down event. All other events should be preceded by this.
     *
     * @param event The down motion event.
     */
    default boolean onDown(MotionEvent event) {return false;}

    /**
     * The user has performed a down {@link MotionEvent} and not performed
     * a move or up yet. This event is commonly used to provide visual
     * feedback to the user to let them know that their action has been
     * recognized i.e. highlight an element.
     *
     * @param event The down motion event
     */
    default void onShowPress(MotionEvent event) {}

    /**
     * Notified when a tap occurs with the up {@link MotionEvent}
     * that triggered it.
     *
     * @param event The up motion event that completed the first tap
     * @return true if the event is consumed, else false
     */
    default boolean onSingleTapUp(MotionEvent event) {return false;}

    /**
     * Notified when a scroll occurs with the initial on down {@link MotionEvent} and the
     * current move {@link MotionEvent}. The distance in x and y is also supplied for
     * convenience.
     *
     * @param event1 The first down motion event that started the scrolling.
     * @param event2 The move motion event that triggered the current onScroll.
     * @param distanceX The distance along the X axis that has been scrolled since the last
     *              call to onScroll. This is NOT the distance between {@code e1}
     *              and {@code e2}.
     * @param distanceY The distance along the Y axis that has been scrolled since the last
     *              call to onScroll. This is NOT the distance between {@code e1}
     *              and {@code e2}.
     * @return true if the event is consumed, else false
     */
    default boolean onScroll(MotionEvent event1, MotionEvent event2,
                             float distanceX, float distanceY) {return false;}

    /**
     * Notified when a long press occurs with the initial on down {@link MotionEvent}
     * that triggered it.
     *
     * @param event The initial on down motion event that started the longpress.
     */
    default void onLongPress(MotionEvent event) {}

    /**
     * Notified of a fling event when it occurs with the initial on down {@link MotionEvent}
     * and the matching up {@link MotionEvent}. The calculated velocity is supplied along
     * the x and y axis in pixels per second.
     *
     * @param event1 The first down motion event that started the fling.
     * @param event2 The move motion event that triggered the current onFling.
     * @param velocityX The velocity of this fling measured in pixels per second
     *              along the x axis.
     * @param velocityY The velocity of this fling measured in pixels per second
     *              along the y axis.
     * @return true if the event is consumed, else false
     */
    default boolean onFling(MotionEvent event1, MotionEvent event2,
                            float velocityX, float velocityY) {return false;}

    /**
     * Notified when a context click occurs.
     *
     * @param event The motion event that occurred during the context click.
     * @return true if the event is consumed, else false
     */
    default boolean onContextClick(MotionEvent event) {return false;}

    /**
     * Notified when a single-tap occurs.
     * <p>
     * Unlike {@link OnGestureListener#onSingleTapUp(MotionEvent)}, this
     * will only be called after the detector is confident that the user's
     * first tap is not followed by a second tap leading to a double-tap
     * gesture.
     *
     * @param event The down motion event of the single-tap.
     */
    default void onSingleTapConfirmed(MotionEvent event) {}

    /**
     * Notified when a double-tap occurs.
     *
     * @param event The down motion event of the first tap of the double-tap.
     * @return true if the event is consumed, else false
     */
    default boolean onDoubleTap(MotionEvent event) {return false;}

    /**
     * Notified when an event within a double-tap gesture occurs, including
     * the down, move, and up events.
     *
     * @param event The motion event that occurred during the double-tap gesture.
     * @return true if the event is consumed, else false
     */
    default boolean onDoubleTapEvent(MotionEvent event) {return false;}
  }
}

