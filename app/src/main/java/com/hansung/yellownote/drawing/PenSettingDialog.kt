package com.hansung.yellownote.drawing

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.hansung.notedatabase.MyDAO
import com.hansung.yellownote.R

class PenSettingDialog(context:Context) {
    private val dialog = Dialog(context)
    private lateinit var widthTextKor: TextView
    private lateinit var widthText: TextView
    private lateinit var widthSeekBar: SeekBar
    private var selectedWidth = 0
    private lateinit var penInfo: PenInfo


    @RequiresApi(Build.VERSION_CODES.O)
    fun show(myDAO: MyDAO, penInfo: PenInfo, x:Int, y:Int){
        dialog.apply {
            setContentView(R.layout.pen_popup)
            window!!.setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )

            var Measuredwidth = 0
            var Measuredheight = 0
            val size = Point()
            val windowManager = window!!.windowManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                windowManager.defaultDisplay.getSize(size)
                Measuredwidth = size.x
                Measuredheight = size.y
            } else {
                val d = windowManager.defaultDisplay
                Measuredwidth = d.getWidth()
                Measuredheight = d.getHeight()
            }

            window!!.attributes.x = -Measuredwidth/2+x+100
            window!!.attributes.y = -Measuredheight/2+y+120
            window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            setCanceledOnTouchOutside(true)
            setCancelable(true)

            show()
        }

        widthTextKor = dialog.findViewById<TextView>(R.id.widthKor)
        widthText = dialog.findViewById<TextView>(R.id.widthTextView)
        widthSeekBar = dialog.findViewById<SeekBar>(R.id.penWidthSeekbar)

        widthSeekBar.min = 5
        widthSeekBar.max = 30
        widthSeekBar.progress = myDAO.getAllPenData()[0].width.toInt()
        widthText.text = "${ widthSeekBar.progress }px"

        widthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                widthText.text = "${p1}px"
                selectedWidth = p1
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                System.out.println("${selectedWidth}px")
                penInfo.setPenWidth(selectedWidth.toFloat())
                if(penInfo.getPenColor()!=null) // 펜인 경우
                    myDAO.updatePenData("PEN",penInfo.getPenWidth(),penInfo.getPenColor(),true)
                else // 지우개인 경우
                    myDAO.updatePenData("ERASER",penInfo.getPenWidth(),null,true)
            }
        })
    }

    fun changeText(){
        widthTextKor.text = "지우개 굵기"
    }

    private fun getSelectedWidth():Int{
        return selectedWidth
    }

    fun isDialogShowing():Boolean {
        return dialog.isShowing
    }

    fun dismiss(){
        if(dialog.isShowing)
            dialog.dismiss()
    }
}