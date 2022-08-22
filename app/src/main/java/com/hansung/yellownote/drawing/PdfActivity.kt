package com.hansung.yellownote.drawing

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import com.hansung.yellownote.MqttAdapter
import com.hansung.yellownote.databinding.ActivityPdfBinding
import org.eclipse.paho.client.mqttv3.MqttClient
import java.io.File

class PdfActivity() : AppCompatActivity(){
//class PdfActivity() : AppCompatActivity(){
    lateinit var binding : ActivityPdfBinding
    private var pdfReader: PdfReader? = null
    lateinit var viewPager:ViewPager2
    private lateinit var filePath : String
    lateinit var redBtn:ImageButton
    lateinit var yellowBtn:ImageButton
    lateinit var greenBtn:ImageButton
    lateinit var blueBtn:ImageButton
    lateinit var blackBtn:ImageButton
    lateinit var eraserBtn:ImageButton
    lateinit var clippingBtn:ImageButton
    lateinit var textBtn:ImageButton
    lateinit var recordBtn:ImageButton
    var pageNo = 0

    val client:MqttAdapter=MqttAdapter()

    // DrawingView.kt에서 정의된 mode와 같아야함
    val PEN = 0
    val ERASER = 1
    val TEXT = 2
    val CLIPPING = 3
    val SHAPE = 4



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.out.println("onCreate")
        binding = ActivityPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewPager = binding.viewPager

        viewPager.adapter = PageAdaptor()

        filePath = intent.getStringExtra("filePath")!!
        val targetFile = File(filePath)

        pdfReader = PdfReader(targetFile, filePath, viewPager,client).apply {
//        pdfReader = PdfReader(targetFile, filePath, viewPager).apply {
            (viewPager.adapter as PageAdaptor).setupPdfRenderer(this)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(page: Int) {
                super.onPageSelected(page)
                pageNo = page

                // position에 해당하는 pageInfo 가져오기
                if(!pdfReader!!.pageInfoMap.containsKey(page)) // page에 해당하는 pageInfo가 없는 경우
                    pdfReader!!.pageInfoMap[page] = PageInfo(page) // 새로운 pageInfo 생성

                pdfReader!!.pageInfoMap[page]?.let { pdfReader!!.setDrawingViewPageInfo(it) } // 변경된 page의 pageInfo 세팅
//                System.out.println("Page$position path개수 = ${pdfReader!!.pageInfoMap[position]?.customPaths?.size}")
            }
        })

        redBtn = binding.RedPenBtn
        yellowBtn = binding.YellowPenBtn
        greenBtn = binding.GreenPenBtn
        blueBtn = binding.BluePenBtn
        blackBtn = binding.BlackPenBtn
        clippingBtn = binding.ClippingBtn
        eraserBtn = binding.EraserBtn
        textBtn = binding.TextBtn


        redBtn.setOnClickListener {
            System.out.println("Click redBtn")
            pdfReader!!.setDrawingMode(PEN,Color.RED)
//            pdfReader!!.pageInfoMap[pageNo]?.penColor = Color.RED
//            pdfReader!!.setColor(Color.RED)
        }
        yellowBtn.setOnClickListener {
            System.out.println("Click yellowBtn")
            pdfReader!!.setDrawingMode(PEN,Color.YELLOW)
        }
        greenBtn.setOnClickListener {
            System.out.println("Click greenBtn")
            pdfReader!!.setDrawingMode(PEN,Color.GREEN)
        }
        blueBtn.setOnClickListener {
            System.out.println("Click blueBtn")
            pdfReader!!.setDrawingMode(PEN,Color.BLUE)
        }
        blackBtn.setOnClickListener {
            System.out.println("Click blackBtn")
            pdfReader!!.setDrawingMode(PEN,Color.BLACK)
        }
        eraserBtn.setOnClickListener {
            pdfReader!!.setDrawingMode(ERASER)
        }
        clippingBtn.setOnClickListener {
            pdfReader!!.setDrawingMode(CLIPPING)
        }
        textBtn.setOnClickListener {
//            pdfReader!!.setMode("text")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfReader?.close()
    }
}