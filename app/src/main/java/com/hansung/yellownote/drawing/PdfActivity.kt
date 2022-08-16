package com.hansung.yellownote.drawing

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import com.hansung.yellownote.databinding.ActivityPdfBinding
import java.io.File

class PdfActivity : AppCompatActivity(){
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
    lateinit var textBtn:ImageButton
    lateinit var recordBtn:ImageButton
    var pageNo = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.out.println("onCreate")
        binding = ActivityPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewPager = binding.viewPager

        viewPager.adapter = PageAdaptor()

        filePath = intent.getStringExtra("filePath")!!
        val targetFile = File(filePath)

        pdfReader = PdfReader(targetFile, filePath, viewPager).apply {
            (viewPager.adapter as PageAdaptor).setupPdfRenderer(this)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                pageNo = position
//                System.out.println("position = ${position}")

                // position에 해당하는 pageInfo 가져오기
                if(!pdfReader!!.pageInfoMap.containsKey(position)){ // position에 해당하는 pageInfo가 없는 경우
//                    System.out.println("page${position}의 pageInfo 생성")
                    pdfReader!!.pageInfoMap[position] = PageInfo(position) // pageInfo 생성
//                    System.out.println("${pdfReader!!.pageInfoMap[position]?.pageNo}")
                }
                pdfReader!!.pageInfoMap[position]?.let { pdfReader!!.setDrawingViewPageInfo(it) }
//                System.out.println("Page$position path개수 = ${pdfReader!!.pageInfoMap[position]?.customPaths?.size}")
            }
        })

        redBtn = binding.RedPenBtn
        yellowBtn = binding.YellowPenBtn
        greenBtn = binding.GreenPenBtn
        blueBtn = binding.BluePenBtn
        blackBtn = binding.BlackPenBtn
        eraserBtn = binding.EraserBtn
        textBtn = binding.TextBtn


        redBtn.setOnClickListener {
            System.out.println("Click redBtn")
            pdfReader!!.setMode("pen")
//            pdfReader!!.pageInfoMap[pageNo]?.penColor = Color.RED
            pdfReader!!.setColor(Color.RED)
        }
        yellowBtn.setOnClickListener {
            System.out.println("Click yellowBtn")
            pdfReader!!.setMode("pen")
//            pdfReader!!.pageInfoMap[pageNo]?.penColor = Color.YELLOW
            pdfReader!!.setColor(Color.YELLOW)
        }
        greenBtn.setOnClickListener {
            System.out.println("Click greenBtn")
            pdfReader!!.setMode("pen")
//            pdfReader!!.pageInfoMap[pageNo]?.penColor = Color.GREEN
            pdfReader!!.setColor(Color.GREEN)
        }
        blueBtn.setOnClickListener {
            System.out.println("Click blueBtn")
            pdfReader!!.setMode("pen")
//            pdfReader!!.pageInfoMap[pageNo]?.penColor = Color.BLUE
            pdfReader!!.setColor(Color.BLUE)
        }
        blackBtn.setOnClickListener {
            System.out.println("Click blackBtn")
            pdfReader!!.setMode("pen")
//            pdfReader!!.pageInfoMap[pageNo]?.penColor = Color.BLACK
            pdfReader!!.setColor(Color.BLACK)
        }
        eraserBtn.setOnClickListener {
            pdfReader!!.setMode("eraser")
        }
        textBtn.setOnClickListener {
            pdfReader!!.setMode("text")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfReader?.close()
    }
}