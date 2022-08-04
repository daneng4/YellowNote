package com.hansung.yellownote

import android.gesture.GestureOverlayView
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import com.example.pdfrenderer.PdfReader
import com.hansung.yellownote.databinding.ActivityPdfBinding
import java.io.File

class PdfActivity : AppCompatActivity(){
    lateinit var binding : ActivityPdfBinding
    private var pdfReader: PdfReader? = null
    lateinit var pdf_view_pager:ViewPager2
    private lateinit var filePath : String
    lateinit var redBtn:ImageButton
    lateinit var yellowBtn:ImageButton
    lateinit var greenBtn:ImageButton
    lateinit var blueBtn:ImageButton
    lateinit var blackBtn:ImageButton
    lateinit var eraserBtn:ImageButton
    lateinit var textBtn:ImageButton
    lateinit var recordBtn:ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pdf_view_pager = binding.pdfViewPager

        pdf_view_pager.adapter = PageAdaptor()

        filePath = intent.getStringExtra("filePath")!!
        val targetFile = File(filePath)
//        System.out.println("intent.getStringExtra = "+filePath)

//        manageDrawing = ManageDrawing(filePath,pdf_view_pager)
        pdfReader = PdfReader(targetFile, filePath, pdf_view_pager).apply {
            (pdf_view_pager.adapter as PageAdaptor).setupPdfRenderer(this)
        }

//        pdfReader = PdfReader(targetFile, filePath, pdf_view_pager)
//        pdfReader!!.setPageChangeCallback()

////        pdf_view_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
////            override fun onPageSelected(position: Int) {
////                super.onPageSelected(position)
////                System.out.println("position = ${position}")
//            }
//        })


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
            pdfReader!!.setColor(Color.RED)
        }
        yellowBtn.setOnClickListener {
            System.out.println("Click yellowBtn")
            pdfReader!!.setMode("pen")
            pdfReader!!.setColor(Color.YELLOW)
        }
        greenBtn.setOnClickListener {
            System.out.println("Click greenBtn")
            pdfReader!!.setMode("pen")
            pdfReader!!.setColor(Color.GREEN)
        }
        blueBtn.setOnClickListener {
            System.out.println("Click blueBtn")
            pdfReader!!.setMode("pen")
            pdfReader!!.setColor(Color.BLUE)
        }
        blackBtn.setOnClickListener {
            System.out.println("Click blackBtn")
            pdfReader!!.setMode("pen")
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