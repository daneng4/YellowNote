package com.example.photoview

import android.content.Context
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ScaleGestureDetector.OnScaleGestureListener
import java.lang.Exception
import java.lang.IllegalArgumentException

internal class CustomGestureDetector(context: Context?, listener: OnGestureListener) {
    private var mActivePointerId = INVALID_POINTER_ID
    private var mActivePointerIndex = 0
    private val mDetector // 드래그 및 확대
            : ScaleGestureDetector
    private var mVelocityTracker // 드래그 속도 처리
            : VelocityTracker? = null
    var isDragging = false
        private set
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private val mTouchSlop: Float
    private val mMinimumVelocity: Float
    private val mListener: OnGestureListener
    private fun getActiveX(ev: MotionEvent): Float {
        return try {
            ev.getX(mActivePointerIndex) // 최신 x좌표
        } catch (e: Exception) {
            ev.x
        }
    }

    private fun getActiveY(ev: MotionEvent): Float {
        return try {
            ev.getY(mActivePointerIndex) // 최신 y좌표
        } catch (e: Exception) {
            ev.y
        }
    }

    val isScaling: Boolean
        get() = mDetector.isInProgress

    fun onTouchEvent(ev: MotionEvent): Boolean {
        return try {
            mDetector.onTouchEvent(ev)
            processTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            // Fix for support lib bug, happening when onDestroy is called
            true
        }
    }

    private fun processTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = ev.getPointerId(0)
                mVelocityTracker = VelocityTracker.obtain()
                if (null != mVelocityTracker) {
                    mVelocityTracker!!.addMovement(ev) // 사용자 움직임 추가
                }
                mLastTouchX = getActiveX(ev)
                mLastTouchY = getActiveY(ev)
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val x = getActiveX(ev)
                val y = getActiveY(ev)
                val dx = x - mLastTouchX
                val dy = y - mLastTouchY
                if (!isDragging) {
                    // Use Pythagoras to see if drag length is larger than
                    // touch slop
                    isDragging = Math.sqrt((dx * dx + dy * dy).toDouble()) >= mTouchSlop
                }
                if (isDragging) {
                    mListener.onDrag(dx, dy)
                    mLastTouchX = x
                    mLastTouchY = y
                    if (null != mVelocityTracker) {
                        mVelocityTracker!!.addMovement(ev)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
                // Recycle Velocity Tracker
                if (null != mVelocityTracker) {
                    mVelocityTracker!!.recycle()
                    mVelocityTracker = null
                }
            }
            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
                if (isDragging) {
                    if (null != mVelocityTracker) {
                        mLastTouchX = getActiveX(ev)
                        mLastTouchY = getActiveY(ev)

                        // Compute velocity within the last 1000ms
                        mVelocityTracker!!.addMovement(ev)
                        mVelocityTracker!!.computeCurrentVelocity(1000)
                        val vX = mVelocityTracker!!.xVelocity
                        val vY = mVelocityTracker!!
                            .yVelocity

                        // If the velocity is greater than minVelocity, call
                        // listener
                        if (Math.max(Math.abs(vX), Math.abs(vY)) >= mMinimumVelocity) {
                            mListener.onFling(
                                mLastTouchX, mLastTouchY, -vX,
                                -vY
                            )
                        }
                    }
                }

                // Recycle Velocity Tracker
                if (null != mVelocityTracker) {
                    mVelocityTracker!!.recycle()
                    mVelocityTracker = null
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = Util.getPointerIndex(ev.action)
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mActivePointerId = ev.getPointerId(newPointerIndex)
                    mLastTouchX = ev.getX(newPointerIndex)
                    mLastTouchY = ev.getY(newPointerIndex)
                }
            }
        }
        mActivePointerIndex = ev
            .findPointerIndex(if (mActivePointerId != INVALID_POINTER_ID) mActivePointerId else 0)
        return true
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
    }

    init {
        val configuration = ViewConfiguration
            .get(context!!)
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity.toFloat()
        //getScaledMinimumFlingVelocity() = 초당 픽셀로 측정된 플링을 시작하기 위한 최소 속도
        mTouchSlop = configuration.scaledTouchSlop.toFloat()
        //getScaledTouchSlop() = 사용자가 스크롤하고 있다고 생각하기 전에 터치가 이동할 수 있는 거리
        mListener = listener
        val mScaleListener: OnScaleGestureListener = object : OnScaleGestureListener {
            private var lastFocusX = 0f
            private var lastFocusY = 0f
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                if (java.lang.Float.isNaN(scaleFactor) || java.lang.Float.isInfinite(scaleFactor)) //Float.isNaN(scaleFactor) = scaleFactor 값이 Not a Number 이면  True 반환 else False
                //Float.isInfinite(scaleFactor) = scaleFactor 값의 크기가 무한히 크면 true를 반환하고 그렇지 않으면 false를 반환
                    return false
                if (scaleFactor >= 0) { // 진행중인 제스처에 대한 크기 조정, 얼마나 확대/축소하는지..
                    mListener.onScale(
                        scaleFactor,
                        detector.focusX,
                        detector.focusY,
                        detector.focusX - lastFocusX,
                        detector.focusY - lastFocusY
                    )
                    lastFocusX = detector.focusX
                    lastFocusY = detector.focusY
                }
                return true
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean { // 크기 조정 제스처 시작응답
                lastFocusX = detector.focusX
                lastFocusY = detector.focusY
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) { // 스케일 제스처 끝 응답
                // NO-OP
            }
        }
        mDetector = ScaleGestureDetector(context, mScaleListener)
    }
}