package com.hansung.yellownote.drawing

import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import java.util.ArrayList

class CustomPath(startPoint: PointF, color: Int, thickness: Float){
    lateinit var points : ArrayList<PointF> // customPath가 지나는 point들
    lateinit var endPoint: PointF // 끝점
    private var startPoint : PointF // 시작점
    private var color : Int = Color.BLACK// 펜 색상
    private var thickness : Float // 펜 굵기

    init{
        this.startPoint = startPoint
        this.color = color
        this.thickness = thickness
        points = ArrayList<PointF>()
    }

    fun addPoint(point: PointF){
        points.add(point)
    }
}