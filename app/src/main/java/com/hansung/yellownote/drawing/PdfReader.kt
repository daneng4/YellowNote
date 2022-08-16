package com.hansung.yellownote.drawing

import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.MotionEvent
import android.widget.ImageView
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
    var pageInfoMap = HashMap<Int,PageInfo>()
    lateinit var pageInfo:PageInfo
    var penColor = Color.BLACK

    fun openPage(page: Int, drawingView: DrawingView) {
        if (page >= pageCount) return
        this.drawingView = drawingView
        drawingView.pdfReader = this
        drawingView.viewPager2 = view_pager
        this.page = page
        currentPage?.close()
//        currentPage = pdfRenderer.openPage(page)

        currentPage = pdfRenderer.openPage(page).apply {
//            System.out.println("${view_pager.width}x${view_pager.height}")
            var pageRatio = width/(height).toDouble()
            var backgroundWidth = width
            var backgroundHeight = height

            System.out.println("view_pager : ${view_pager.width}x${view_pager.height}")
            System.out.println("currentPage : ${width}x${height}")

//            if(pageRatio>1){

            // view_pager에 맞춰서 배경될 pdf 크기 변경
            backgroundWidth = view_pager.width
            backgroundHeight = (view_pager.width/pageRatio).toInt()
            drawingView.getLayoutParams().height = (view_pager.width/pageRatio).toInt()
            if(backgroundHeight>view_pager.height){
                backgroundWidth = (view_pager.height*pageRatio).toInt()
                backgroundHeight = view_pager.height
                drawingView.layoutParams.width = backgroundWidth
                drawingView.layoutParams.height = view_pager.height
            }

//            }
            backgroundBitmap = Bitmap.createBitmap(
                backgroundWidth,backgroundHeight, Bitmap.Config.ARGB_8888
            )
            System.out.println("backgroundBitmap : ${backgroundWidth}x${backgroundHeight}")
//            else{ // pdf원본 페이지의 세로>가로
//                backgroundWidth = (view_pager.height*pageRatio).toInt()
//                backgroundHeight = view_pager.height
//                backgroundBitmap = Bitmap.createBitmap(
//                    backgroundWidth,view_pager.height, Bitmap.Config.ARGB_8888
//                )
//                drawingView.getLayoutParams().width = (view_pager.height*pageRatio).toInt();
//            }
//            backgroundBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            render(backgroundBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            drawingView.setImageBitmap(backgroundBitmap)
            drawingView.invalidate()

            drawingView.createDrawingBitmap(backgroundBitmap)
        }
    }

    fun setDrawingViewPageInfo(pageInfo: PageInfo){
//        System.out.println("${pageInfo.page} pageInfo의 page번호 = ${pageInfo.pageNo}")
        this.pageInfo = pageInfo
        drawingView.changePageInfo(pageInfo)
//        pageInfo.drawingView = drawingView
        System.out.println("${pageInfo.pageNo}의 drawingView ${pageInfo.drawingView}")
    }

    fun setMode(mode:String){
        drawingView.drawingMode = mode
        if(mode.equals("eraser"))
            drawingView.setErase()
    }

    fun setColor(color:Int){
        System.out.println("${drawingView}의 펜 색 변경")
        this.penColor = color
        pageInfo.drawingView.setPenStyle(color,10F)
        pageInfo.drawingView.penColor = color
    }

    fun close() {
        currentPage?.close()
        fileDescriptor.close()
        pdfRenderer.close()
    }
}