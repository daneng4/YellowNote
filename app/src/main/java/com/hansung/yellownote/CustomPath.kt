package com.hansung.yellownote

import android.graphics.Color
import android.graphics.Point
import java.util.ArrayList

class CustomPath(startPoint: Point, color: Int, thickness:Int){
    lateinit var points : ArrayList<Point> // customPath가 지나는 point들
    lateinit var endPoint: Point // 끝점
    private var startPoint : Point // 시작점
    private var color : Int = Color.BLACK// 펜 색상
    private var thickness = 0 // 펜 굵기

    init{
        this.startPoint = startPoint
        this.color = color
        this.thickness = thickness
        points = ArrayList<Point>()
    }

    fun addPoint(point: Point){
        points.add(point)
    }
}