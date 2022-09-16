package com.hansung.yellownote.drawing

import android.graphics.Color
import java.nio.file.Paths

class PageInfo(pageNo:Int) {
    var pageNo = pageNo
    var customPaths = ArrayList<CustomPath>()

    fun removeSelectedPaths(selectedPaths: ArrayList<CustomPath>){
        for(i in 0..selectedPaths.size-1){
            customPaths.remove(selectedPaths[i])
        }
        System.out.println(customPaths.size)
    }
}