package com.hansung.yellownote.drawing

import android.graphics.*
import java.util.ArrayList

class CustomPath(startPoint: PointF){
    lateinit var path : Path
    var points : ArrayList<PointF> // customPath가 지나는 point들
    lateinit var endPoint: PointF // 끝점
    private var startPoint : PointF // 시작점
    lateinit var drawingPaint: Paint

    init{
        this.startPoint = startPoint
        points = ArrayList<PointF>()
    }

    // point 추가
    fun addPoint(point: PointF){
        points.add(point)
    }

    // point들 위치 변경하기
    fun changePointPosition(offsetX:Float, offsetY:Float){
        startPoint.x += offsetX
        startPoint.y += offsetY
        endPoint.x += offsetX
        endPoint.y += offsetY

        for(i in 0..points.size-1){
            points[i].x += offsetX
            points[i].y += offsetY
        }
    }
    fun setPoints(point: PointF){
        points.add(point)
    }
}