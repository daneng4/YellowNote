package com.hansung.yellownote.drawing

import android.graphics.*
import androidx.room.Embedded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.util.ArrayList

class CustomPath(startPoint: PointF){
    lateinit var path : SerializablePath
    var points : ArrayList<PointF> // customPath가 지나는 point들
    lateinit var endPoint: PointF // 끝점
    var startPoint : PointF // 시작점
    lateinit var drawingPaint: Paint
    lateinit var pathToByte :ByteArray

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

    fun changeToByte(){
        CoroutineScope(Dispatchers.Default).apply {
            launch {
                System.out.println("11111111111111111111111111111111")
                var baos = ByteArrayOutputStream()
                var objectOutput = ObjectOutputStream(baos)
                objectOutput.writeObject(path)
                pathToByte = baos.toByteArray()
                System.out.println("aaaa = ${pathToByte.toString()}")
            }
        }
    }

    fun ByteToPath(){
        CoroutineScope(Dispatchers.Default).apply {
            launch {
                System.out.println("2222222222222222222222222222222")
                var obis = ObjectInputStream(ByteArrayInputStream(pathToByte))
                path = obis.readObject() as SerializablePath

                System.out.println("path.isEmpty = ${path.isEmpty}")
            }
        }
    }
}