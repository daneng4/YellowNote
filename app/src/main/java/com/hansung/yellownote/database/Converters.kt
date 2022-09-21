package com.hansung.yellownote.database

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.pdf.PdfDocument
import android.widget.EditText
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
    fun convertCustomPathsToJson(customPaths: ArrayList<CustomPath>): String? {
        val list=customPaths.toList()
        return Gson().toJson(list)
    }
    @TypeConverter
    fun convertCustomEditTextToJson(customEditText: CustomEditText):String?{
        return Gson().toJson(customEditText)
    }
    @TypeConverter
    fun convertCustomEditTextArrayToJson(textArray:ArrayList<CustomEditText>):String?{
        return Gson().toJson(textArray)
    }
    @TypeConverter
    fun convertJsonToCustomEditText(json: String):CustomEditText{
        return Gson().fromJson(json,CustomEditText::class.java)
    }
    @TypeConverter
    fun convertJsonToCustomEditTextArray(json:String):ArrayList<CustomEditText>{
        val tmp=Gson().fromJson(json,Array<CustomEditText>::class.java).toList()
        val texts=ArrayList<CustomEditText>()
        for(text in tmp){
            texts.add(text)
        }
        return texts
    }
    @TypeConverter
    fun jsonCustomPaths(json:String): ArrayList<CustomPath> {
        val tmp=Gson().fromJson(json,Array<CustomPath>::class.java).toList()
        val customPaths= java.util.ArrayList<CustomPath>()

        var redrawPath = Path()
        CoroutineScope(Dispatchers.Main).launch {
            for(customPath in tmp){
                customPath.drawingPaint = Paint().apply{
                    color = customPath.penColor
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = customPath.penWidth
                }
                redrawPath = Path()
                redrawPath.moveTo(customPath.startPoint.x,customPath.startPoint.y)
                for(point in customPath.points)
                    redrawPath.lineTo(point.x,point.y)
                redrawPath.lineTo(customPath.endPoint.x, customPath.endPoint.y)
                customPath.path = redrawPath
                customPaths.add(customPath)
            }
        }
        return customPaths
    }
}