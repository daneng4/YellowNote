package com.hansung.yellownote.drawing

import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.viewpager2.widget.ViewPager2
import com.hansung.notedatabase.FileData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class PdfReader(file: File, filePath: String, view_pager:ViewPager2) {
    private var currentPage: PdfRenderer.Page? = null
    private val fileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    val view_pager = view_pager

    private var page = 0
    private lateinit var backgroundBitmap:Bitmap

    val pageCount = pdfRenderer.pageCount
    var firstOpen=true
    lateinit var drawingView: DrawingView
    var pageInfoMap = HashMap<Int,PageInfo>() // <page번호, PageInfo>
    lateinit var pageInfo:PageInfo // 현재 page의 PageInfo

    var pen = Pen()

    // DrawingView.kt에서 정의된 mode와 같아야함
    val NONE = -1
    val PEN = 0
    val HIGHLIGHTER = 1
    val ERASER = 2
    val TEXT = 3
    val CLIPPING = 4
    val MOVING = 5
    var drawingMode = PEN

    fun openPage(page: Int, drawingView: DrawingView) {
        System.out.println("OpenPage")
        if (page >= pageCount) return
        this.drawingView = drawingView
        drawingView.pdfReader = this
        drawingView.viewPager2 = view_pager
        this.page = page
        currentPage?.close()
        println("pdfRender page: $page")
        currentPage = pdfRenderer.openPage(page).apply {
            var pageRatio = width/(height).toDouble()
            System.out.println("view_pager : ${view_pager.width}x${view_pager.height}")
            System.out.println("currentPage : ${width}x${height}")

            if(pageInfoMap[page]!=null){
                drawingView.pageInfo = pageInfoMap[page]
                CoroutineScope(Dispatchers.Main).launch{
                    System.out.println("drawingView.pageInfo!! = ${drawingView.pageInfo!!}")
                    for(i in 0..drawingView.pageInfo!!.customPaths.size-1){
                        var customPath = pageInfo.customPaths[i]
                        drawingView.canvas.drawPath(customPath.path, customPath.drawingPaint)
                    }
                    drawingView.invalidate()
                }
            }

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
            //setDrawingViewPageInfo(pageInfoMap[page]!!
        }
        println("pdfReader openpdf end")
    }
    fun setPageNumberToPageInfo(page:Int){
        println("setPageNumberToPageInfo")
        if(pageInfoMap[page]!=null)
            pageInfo=pageInfoMap[page]!!
    }

    fun makePageInfoMap(fileDatas:List<FileData>){
        if(fileDatas.isEmpty()){
            return
        }
        for(data in fileDatas){
            val pageInfo=PageInfo(data.drawingInfo.pageNo)
            pageInfo.setCustomPaths(data.drawingInfo.customPaths)
            pageInfo.changePathColor(data.drawingInfo.penColor!!)
            pageInfoMap[data.drawingInfo.pageNo]=pageInfo
            System.out.println("pageInfo.pageNo = ${pageInfo.pageNo} / pageInfo.customPaths = ${pageInfo.customPaths}")
        }
        System.out.println("PageInfoMap = ${pageInfoMap.keys}")
    }

    fun changePageInfo(pageInfo: PageInfo){ // 현재 page에 맞는 pageInfo 세팅
        this.pageInfo = pageInfo
        println(this.pageInfo)
        drawingView.changePageInfo(pageInfo)
    }

    fun close() {
        currentPage?.close()
        fileDescriptor.close()
        pdfRenderer.close()
    }
}