package cz.cubeit.cubeit

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import android.os.VibrationEffect
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import android.os.Vibrator



open class Class_OnSwipeTouchListener(c: Context, val view: View, val longPressable: Boolean) : View.OnTouchListener {

    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    private val gestureDetector: GestureDetector

    init {
        gestureDetector = GestureDetector(c, GestureListener())
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if(!view.isClickable) return false

        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            view.isPressed = true
            onDownMotion()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            return super.onSingleTapConfirmed(e)
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick(e.rawX, e.rawY)
            return super.onSingleTapUp(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleClick()
            return super.onDoubleTap(e)
        }

        override fun onLongPress(e: MotionEvent) {
            onLongClick()
            super.onLongPress(e)
        }

        // Determines the fling velocity and then fires the appropriate swipe event accordingly
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val result = false
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                } else {
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeDown()
                        } else {
                            onSwipeUp()
                        }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

            return result
        }
    }

    open fun onDownMotion() {}

    open fun onSwipeRight() {
        view.isPressed = false
    }

    open fun onSwipeLeft() {
        view.isPressed = false
    }

    open fun onSwipeUp() {
        view.isPressed = false
    }

    open fun onSwipeDown() {
        view.isPressed = false
    }

    open fun onClick(x: Float, y: Float) {
        view.isPressed = false
    }

    open fun onDoubleClick() {
        view.isPressed = false
    }

    open fun onLongClick() {
        view.isPressed = false
        if(Data.player.vibrateEffects && longPressable){
            val v = view.context.getSystemService(VIBRATOR_SERVICE) as Vibrator?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v!!.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                v!!.vibrate(10)
            }
        }
    }
}