package com.hansung.yellownote.drawing

import android.graphics.Color
import android.widget.EditText
import androidx.room.Embedded
import java.io.Serializable
import java.nio.file.Paths


class PageInfo(pageNo:Int) {
    var pageNo = pageNo
    var customPaths = ArrayList<CustomPath>()
    var customEditText=ArrayList<CustomEditText>()
    var penColor:Int? = Color.BLACK
    var customEditText=ArrayList<CustomEditText>()

    fun changePathColor(penColor:Int){
        this.penColor = penColor
    }

    fun removeSelectedPaths(selectedPaths: ArrayList<CustomPath>){
        for(i in 0..selectedPaths.size-1){
            customPaths.remove(selectedPaths[i])
        }
        System.out.println(customPaths.size)
    }
    @JvmName("setCustomPaths1")
    fun setCustomPaths(paths: ArrayList<CustomPath>){
        customPaths=paths
    }
    @JvmName("setCustomEditText1")
    fun setCustomEditText(editText: ArrayList<CustomEditText>){
        customEditText=editText
    }

}