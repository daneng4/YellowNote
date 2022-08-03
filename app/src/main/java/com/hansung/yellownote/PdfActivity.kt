package com.hansung.yellownote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.example.pdfrenderer.PdfReader
import com.hansung.yellownote.databinding.ActivityPdfBinding
import java.io.File

class PdfActivity : AppCompatActivity() {
    private lateinit var binding : ActivityPdfBinding
    private var pdfReader: PdfReader? = null
    private lateinit var pdf_view_pager:ViewPager2
    private lateinit var filePath : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pdf_view_pager = binding.pdfViewPager

        var filePath = intent.getStringExtra("filePath")
        System.out.println("intent.getStringExtra = "+filePath)

        pdf_view_pager.adapter = PageAdaptor()
        val targetFile = File(filePath)
        pdfReader = PdfReader(targetFile).apply {
            (pdf_view_pager.adapter as PageAdaptor).setupPdfRenderer(this)
        }
        /*TabLayoutMediator(pdf_page_tab, pdf_view_pager) {
        tab, position -> tab.text = (position + 1).toString()
        }.attach()*/
    }
    override fun onDestroy() {
        super.onDestroy()
        pdfReader?.close()
    }
}