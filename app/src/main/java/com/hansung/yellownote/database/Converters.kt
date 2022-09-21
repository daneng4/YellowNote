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
}