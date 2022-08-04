package com.example.pdfrenderer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.hansung.yellownote.ManageDrawing
import java.io.File


class PdfReader(file: File, filePath: String, pdf_view_pager:ViewPager2) {
    private var manageDrawings : HashMap<Int,ManageDrawing> = HashMap<Int,ManageDrawing>()
    private lateinit var manageDrawing:ManageDrawing
    private var currentPage: PdfRenderer.Page? = null
    private val fileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    private val filePath = filePath
    val pdf_view_pager = pdf_view_pager
    private var page = 0
    private var penColor:Int = Color.RED

    val pageCount = pdfRenderer.pageCount

    fun openPage(page: Int, pdfImage: ImageView) {
        if (page >= pageCount) return
        this.page = page
        currentPage?.close()
        currentPage = pdfRenderer.openPage(page).apply {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfImage.setImageBitmap(bitmap)
            if(manageDrawings.containsKey(page)){ // page에 해당하는 manageDrawing 있는 경우
                System.out.println("필기 기록 존재")
                manageDrawing = manageDrawings[page]!!
            }
            else{ // page에 해당하는 manageDrawing 없는 경우 (처음 연 경우)
                manageDrawing = ManageDrawing(filePath,pdf_view_pager)
                manageDrawing.page = page
                manageDrawing.pdfBitmap = bitmap
                manageDrawing.createDrawingBitmap(bitmap.width, bitmap.height, pdfImage)
                manageDrawings[page] = manageDrawing
            }
        }
    }

//    fun setPageChangeCallback(){
//        pdf_view_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
//                System.out.println("position = ${position}")
//            }
//        })
//    }

    fun setMode(mode:String){
        manageDrawing.mode = mode
    }

    fun setColor(color:Int){
        manageDrawing.changePenColor(color)
        System.out.println(" = ${page}")
    }


    fun close() {
        currentPage?.close()
        fileDescriptor.close()
        pdfRenderer.close()
    }
}