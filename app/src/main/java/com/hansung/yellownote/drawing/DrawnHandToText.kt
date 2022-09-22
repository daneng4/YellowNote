package com.hansung.yellownote.drawing

import android.graphics.Paint

class DrawnHandToText(text:String, x:Float, y:Float, textPaint: Paint) {
    var x = 0f
    var y = 0f
    var textPaint:Paint
    var text:String = ""

    init{
        this.text = text
        this.x = x
        this.y = y
        this.textPaint = textPaint
    }
}