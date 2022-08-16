package com.hansung.yellownote.drawing

import android.graphics.Color

class PageInfo(pageNo:Int) {
    var pageNo = pageNo
    var customPaths = ArrayList<CustomPath>()
    var penColor:Int = Color.BLACK
    lateinit var drawingView:DrawingView

    fun changePathColor(penColor:Int){
        this.penColor = penColor
    }
}