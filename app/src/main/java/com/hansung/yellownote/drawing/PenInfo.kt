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
    val HIGHLIGHTER = 1
    val ERASER = 2
    val TEXT = 3
    val CLIPPING = 4
    var MovingClipping = false

    init{
        color.value = Color.BLACK
        width.value = 10F
        PenMode.value = PEN
    }

    fun setPenColor(color:Int?){
//        when(this.color.value){
//            Color.BLACK -> System.out.println("BLACK")
//            Color.RED -> System.out.println("RED")
//            Color.YELLOW -> System.out.println("YELLOW")
//            Color.GREEN -> System.out.println("GREEN")
//            Color.BLUE -> System.out.println("BLUE")
//        }
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