package com.hansung.yellownote.drawing

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_page.view.*

class PageHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun openPage(page: Int, pdfReader: PdfReader) {
//        for(text in pdfReader.drawingView.editTexts){
//            pdfReader.drawingView.textLayout.removeView(text)
//        }
//        pdfReader.drawingView.rootLayout.removeView(pdfReader.drawingView.textLayout)
//        pdfReader.drawingView.changePageInfo(pdfReader.drawingView.pageInfo!!)
////        drawingView.changePageInfo(pageInfo)
//        pdfReader.drawingView.setTextLayout()
        pdfReader.openPage(page, itemView.pdf_image as DrawingView)
    }
}