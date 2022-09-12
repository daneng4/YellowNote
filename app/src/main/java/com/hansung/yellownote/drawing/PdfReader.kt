package com.hansung.yellownote.drawing

import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.viewpager2.widget.ViewPager2
import java.io.File


class PdfReader(file: File, filePath: String, view_pager:ViewPager2) {
    private var currentPage: PdfRenderer.Page? = null
    private val fileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    val view_pager = view_pager

    private var page = 0
    private lateinit var backgroundBitmap:Bitmap

    val pageCount = pdfRenderer.pageCount

    lateinit var drawingView: DrawingView
    var pageInfoMap = HashMap<Int,PageInfo>() // <page번호, PageInfo>
    lateinit var pageInfo:PageInfo // 현재 page의 PageInfo

    var pen = Pen()

    // DrawingView.kt에서 정의된 mode와 같아야함
    val NONE = -1
    val PEN = 0
    val ERASER = 1
    val TEXT = 2
    val CLIPPING = 3
    val SHAPE = 4
    val MOVING = 5
    var drawingMode = PEN

//    init{
//        System.out.println("client 생성")
////        client = MqttAdapter()
//    }

    fun openPage(page: Int, drawingView: DrawingView) {
        if (page >= pageCount) return
        this.drawingView = drawingView
        drawingView.pdfReader = this
        drawingView.viewPager2 = view_pager
        this.page = page
        currentPage?.close()

        currentPage = pdfRenderer.openPage(page).apply {
            var pageRatio = width/(height).toDouble()
            System.out.println("view_pager : ${view_pager.width}x${view_pager.height}")
            System.out.println("currentPage : ${width}x${height}")

            // view_pager에 맞춰서 배경될 pdf 크기 변경
            var backgroundWidth = view_pager.width
            var backgroundHeight = (view_pager.width/pageRatio).toInt()
            drawingView.getLayoutParams().height = (view_pager.width/pageRatio).toInt()
            if(backgroundHeight>view_pager.height){
                backgroundWidth = (view_pager.height*pageRatio).toInt()
                backgroundHeight = view_pager.height
                drawingView.layoutParams.width = backgroundWidth
                drawingView.layoutParams.height = view_pager.height
            }

            backgroundBitmap = Bitmap.createBitmap(
                backgroundWidth,backgroundHeight, Bitmap.Config.ARGB_8888
            )

            render(backgroundBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            drawingView.setImageBitmap(backgroundBitmap)
            drawingView.invalidate()

            drawingView.createDrawingBitmap(backgroundBitmap) // 그림 그릴 bitmap 생성
        }
    }

    fun setDrawingViewPageInfo(pageInfo: PageInfo){ // 현재 page에 맞는 pageInfo 세팅
        this.pageInfo = pageInfo
        drawingView.changePageInfo(pageInfo)
    }

    fun close() {
        currentPage?.close()
        fileDescriptor.close()
        pdfRenderer.close()
    }
}