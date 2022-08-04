package com.example.photoview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.appcompat.widget.AppCompatImageView
import android.widget.ImageView.ScaleType
import android.view.View.OnLongClickListener
import android.graphics.drawable.Drawable
import android.graphics.RectF
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector

class PhotoView @JvmOverloads constructor(
    context: Context?,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(
    context!!, attr, defStyle
) {
    var attacher: PhotoViewAttacher? = null
        private set
    private var pendingScaleType: ScaleType? = null
    private fun init() {
        attacher = PhotoViewAttacher(this)

        super.setScaleType(ScaleType.MATRIX)

        if (pendingScaleType != null) {
            scaleType = pendingScaleType!!
            pendingScaleType = null
        }
    }

    override fun getScaleType(): ScaleType {
        return attacher!!.scaleType
    }

    override fun getImageMatrix(): Matrix {
        return attacher!!.imageMatrix
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        attacher!!.setOnLongClickListener(l)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        attacher!!.setOnClickListener(l)
    }

    override fun setScaleType(scaleType: ScaleType) {
        if (attacher == null) {
            pendingScaleType = scaleType
        } else {
            attacher!!.scaleType = scaleType
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)

        if (attacher != null) {
            attacher!!.update()
        }
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        if (attacher != null) {
            attacher!!.update()
        }
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        if (attacher != null) {
            attacher!!.update()
        }
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val changed = super.setFrame(l, t, r, b)
        if (changed) {
            attacher!!.update()
        }
        return changed
    }

    init {
        init()
    }
}