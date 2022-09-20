package com.hansung.yellownote.drawing

import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import java.nio.file.Paths

class PageInfoViewModel(pageNo:Int): ViewModel() {
    var pageNo = pageNo
    var customPaths = ArrayList<CustomPath>()
    var penColor:Int? = Color.BLACK

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

    @RequiresApi(Build.VERSION_CODES.O)
    fun copyPageInfo(pageInfo: PageInfo){
        this.pageNo = pageInfo.pageNo
        this.customPaths = pageInfo.customPaths
        this.penColor = pageInfo.penColor
    }
}