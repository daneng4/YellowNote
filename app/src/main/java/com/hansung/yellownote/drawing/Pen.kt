package com.hansung.yellownote.drawing

import android.graphics.Color

class Pen {
    var color = Color.BLACK
    var thickness = 10f
    var isStraight = false

    init {
        System.out.println("Pen 생성")
    }
}