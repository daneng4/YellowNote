package com.hansung.yellownote.database

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.pdf.PdfDocument
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.hansung.yellownote.drawing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*

@ProvidedTypeConverter
class Converters {
    @TypeConverter
    fun convertPageInfoToJson(pageInfo: PageInfo): String? {
        return Gson().toJson(pageInfo)
    }
    @TypeConverter
    fun convertCustomPathsToJson(customPaths: ArrayList<CustomPath>): String? {
        val list=customPaths.toList()
        return Gson().toJson(list)
    }
    @TypeConverter
    fun convertPointToJson(pointF: PointF):String?{
        return Gson().toJson(pointF)
    }
    @TypeConverter
    fun convertCustomPathToJson(customPath: CustomPath):String?{
        return Gson().toJson(customPath)
    }
    @TypeConverter
    fun convertPointsToJson(points : ArrayList<PointF>):String?{
        return Gson().toJson(points)
    }
    @TypeConverter
    fun convertDrawingPaintToJson(drawingPaint: Paint): String? {
        return Gson().toJson(drawingPaint)
    }
    @TypeConverter
    fun convertPathToJson(path: Path):String?{
        return Gson().toJson(path)
    }
    @TypeConverter
    fun convertJsonToPoints(json:String):ArrayList<PointF>{
        val tmp=Gson().fromJson(json,Array<PointF>::class.java).toList()
        val points=ArrayList<PointF>()
        for(point in tmp){
            points.add(point)
        }
        return points
    }
    @TypeConverter
    fun convertJsonToPoint(json:String):PointF{
        return Gson().fromJson(json,PointF::class.java)
    }
    @TypeConverter
    fun jsonToPageInfo(json:String): PageInfo {
        return Gson().fromJson(json,PageInfo::class.java)
    }
    @TypeConverter
    fun convertJsonToCustomPath(json:String):CustomPath{
        return Gson().fromJson(json,CustomPath::class.java)
    }
    @TypeConverter
    fun jsonCustomPaths(json:String): ArrayList<CustomPath> {
        val tmp=Gson().fromJson(json,Array<CustomPath>::class.java).toList()
        val customPath= java.util.ArrayList<CustomPath>()
        CoroutineScope(Dispatchers.Main).launch {
            var redrawPath = SerializablePath()
            for(path in tmp){
                redrawPath = SerializablePath()
                redrawPath.moveTo(path.startPoint.x,path.startPoint.y)
                for(i in 0..path.points.size-1)
                    redrawPath.lineTo(path.points[i].x,path.points[i].y)
                redrawPath.lineTo(path.endPoint.x, path.endPoint.y)
                path.path = redrawPath
                customPath.add(path)
            }
        }
        return customPath
    }
    @TypeConverter
    fun jsonToDrawingPaint(json:String):Paint{
        return Gson().fromJson(json,Paint::class.java)
    }
    @TypeConverter
    fun jsonToPath(json:String):Path{
        return Gson().fromJson(json,Path::class.java)
    }
}