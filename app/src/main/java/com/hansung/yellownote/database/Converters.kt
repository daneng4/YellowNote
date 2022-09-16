package com.hansung.yellownote.database

import android.graphics.pdf.PdfDocument
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.hansung.yellownote.drawing.*
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
    fun jsonToPageInfo(json:String): PageInfo {
        return Gson().fromJson(json,PageInfo::class.java)
    }
    @TypeConverter
    fun jsonCustomPaths(json:String): ArrayList<CustomPath> {
        val tmp=Gson().fromJson(json,Array<CustomPath>::class.java).toList()
        val customPath=ArrayList<CustomPath>()
        for(path in tmp){
            customPath.add(path)
        }
        return customPath
    }
}