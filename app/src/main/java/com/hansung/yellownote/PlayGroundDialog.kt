package com.hansung.yellownote

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.ImageView

class PlayGroundDialog(context : Context, mainActivity: MainActivity) {
    private val dialog = Dialog(context)
    private lateinit var AngelImg : ImageView
    private lateinit var VegetableImg : ImageView
    private lateinit var BabyImg : ImageView
    private lateinit var FairyImg : ImageView
    private lateinit var RabbitImg : ImageView
    private lateinit var GirlsImg : ImageView
    private var mainActivity = mainActivity
    private var folderPath = ""

    fun show(){
        folderPath =mainActivity.filesDir.absolutePath + "/PlayGround/"
        System.out.println("${folderPath}")
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
        dialog.setContentView(R.layout.playground_file_chooser)     //다이얼로그에 사용할 xml 파일을 불러옴
        dialog.setCancelable(true)    //다이얼로그의 바깥 화면을 눌렀을 때 다이얼로그가 닫히지 않도록

        AngelImg = dialog.findViewById(R.id.AngelImg)
        VegetableImg = dialog.findViewById(R.id.VegetableImg)
        BabyImg = dialog.findViewById(R.id.BabyImg)
        FairyImg = dialog.findViewById(R.id.FairyImg)
        RabbitImg = dialog.findViewById(R.id.RabbitImg)
        GirlsImg = dialog.findViewById(R.id.GirlsImg)

        AngelImg.setOnClickListener {
            mainActivity.openPDF(folderPath+"angel.pdf")
            dialog.dismiss()
        }
        VegetableImg.setOnClickListener {
            mainActivity.openPDF(folderPath+"vegetables.pdf")
            dialog.dismiss()
        }
        BabyImg.setOnClickListener {
            mainActivity.openPDF(folderPath+"/baby.pdf")
            dialog.dismiss()
        }
        FairyImg.setOnClickListener {
            mainActivity.openPDF(folderPath+"/fairy.pdf")
            dialog.dismiss()
        }
        RabbitImg.setOnClickListener {
            mainActivity.openPDF(folderPath+"/rabbit.pdf")
            dialog.dismiss()
        }
        GirlsImg.setOnClickListener {
            mainActivity.openPDF(folderPath+"/girls.pdf")
            dialog.dismiss()
        }

        dialog.show()
    }
}