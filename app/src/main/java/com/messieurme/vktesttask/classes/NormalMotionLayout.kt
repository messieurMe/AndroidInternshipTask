package com.messieurme.vktesttask.classes

import android.content.Context
import android.util.AttributeSet
import android.view.*
import androidx.constraintlayout.motion.widget.MotionLayout
import com.messieurme.vktesttask.R


class NormalMotionLayout : MotionLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private lateinit var views: List<View>
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        views = listOf(
            R.id.switchUploadMode,
            R.id.addToUpload,
            R.id.pause,
            R.id.description,
            R.id.search_query
        ).map<Int, View> { findViewById(it) }
        return if (views.any { touchEventInsideTargetView(it, ev) }) {
            false
        } else {
            super.onInterceptTouchEvent(ev)
        }
    }

    private fun touchEventInsideTargetView(v: View, ev: MotionEvent): Boolean {
        if (ev.x > v.left && ev.x < v.right) {
            if (ev.y > v.top && ev.y < v.bottom) {
                return true
            }
        }
        return false
    }
}