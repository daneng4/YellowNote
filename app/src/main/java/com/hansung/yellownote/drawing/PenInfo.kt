package com.hansung.yellownote.drawing

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*
import kotlin.collections.ArrayList

class PenInfo : ViewModel() {
    val color:MutableLiveData<Int?> = MutableLiveData<Int?>()
    val width:MutableLiveData<Float> = MutableLiveData<Float>()
    val PenMode:MutableLiveData<Int> = MutableLiveData<Int>()

    val NONE = -1
    val PEN = 0
    val ERASER = 1
    val TEXT = 2
    val CLIPPING = 3
    var MovingClipping = false

    init{
        color.value = Color.BLACK
        width.value = 10F
        PenMode.value = PEN
    }

    fun setPenColor(color:Int?){
        this.color.value = color
    }

    fun getPenColor():Int? {
        return color.value
    }

    fun setPenWidth(width:Float){
        this.width.value = width
    }

    fun getPenWidth():Float {
        return width.value!!
    }

    fun setPenMode(mode:Int){
        this.PenMode.value = mode
    }

    @JvmName("setMovingClipping1")
    fun setMovingClipping(MovingClipping:Boolean){
        this.MovingClipping = MovingClipping
    }

    @JvmName("getMovingClipping1")
    fun getMovingClipping():Boolean{
        return MovingClipping
    }

    fun getPenMode():Int {
        return PenMode.value!!
    }
}