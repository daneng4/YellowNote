//package com.hansung.yellownote
//
//import android.annotation.SuppressLint
//import android.graphics.*
//import android.graphics.pdf.PdfRenderer
//import android.os.Bundle
//import android.os.ParcelFileDescriptor
//import android.view.MotionEvent
//import android.view.View
//import android.view.ViewTreeObserver
//import android.widget.ImageView
//import androidx.appcompat.app.AppCompatActivity
//import com.hansung.yellownote.databinding.ActivityNoteBinding
//import kotlinx.android.synthetic.main.activity_pdf.*
//import java.io.File
//import java.io.IOException
//import java.util.ArrayList
//
//
//class NoteActivity : AppCompatActivity() {
//    private lateinit var binding : ActivityNoteBinding
//    lateinit var pdfView: ImageView
//    lateinit var rendererPage: PdfRenderer.Page
//    var pdfRenderer: PdfRenderer?=null
//    var fileDescriptor:ParcelFileDescriptor?=null
//    var pageCount=0
//    var count=0
//
//    lateinit var customPaths:ArrayList<CustomPath>
//    lateinit var customPath: CustomPath
//    lateinit var path: Path
//    lateinit var canvas: Canvas
//    lateinit var paint: Paint
//    lateinit var EditImagematrix: Matrix
//    var alteredBitmap: Bitmap? = null
//
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding=ActivityNoteBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        pdfView = binding.pdfView
//
//        var intent = intent
//        var filePath = intent.getStringExtra("filePath")
//        System.out.println("intent.getStringExtra = "+filePath)
//
//        if (filePath != null) {
//            openPdf(filePath)
//
//            customPaths = ArrayList<CustomPath>()
//
//            pdfView.setOnTouchListener{ _: View, event:MotionEvent->
//                val x = event.x
//                val y = event.y
//                val toolType=event.getToolType(0)
//                when(toolType) {
//                    MotionEvent.TOOL_TYPE_FINGER-> {
//                        System.out.println("손가락")
////                        pdf_view_pager.isUserInputEnabled = true // 페이지 슬라이드 허용
//                    }
//                    else-> {
//                        when (event.action) {
//                            MotionEvent.ACTION_DOWN -> {
////                                pdf_view_pager.isUserInputEnabled = false // 페이지 슬라이드 금지
//                                System.out.println("펜")
//                                System.out.println("(x,y)=("+x+","+y+")")
//                                customPath = CustomPath(Point(x.toInt(),y.toInt()), Color.RED, 10)
//                                customPaths.add(customPath)
//                                path.reset()
//                                path.moveTo(x, y)
//                            }
//                            MotionEvent.ACTION_MOVE -> {
//                                customPath.addPoint(Point(x.toInt(),y.toInt()))
//                                path.lineTo(x, y)
//                            }
//                            MotionEvent.ACTION_UP -> {
//                                customPath.endPoint = Point(x.toInt(),y.toInt())
//                                path.lineTo(x, y)
//                                canvas.drawPath(path, paint)
//                                path.reset()
//                            }
//                        }
//                    }
//                }
//                pdfView.invalidate()
//                true
//            }
//        }
//    }
//
//    fun openPdf(filePath:String){
//        var file = File(filePath)
//        System.out.println("File(filePath).isFile = "+File(filePath).isFile)
//        System.out.println("File(filePath).canRead = "+File(filePath).canRead())
//
//        if(fileDescriptor==null)
//            fileDescriptor=
//                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
//
//        pdfRenderer=
//            PdfRenderer(fileDescriptor!!)
//
//        pageCount= pdfRenderer?.pageCount!!
//
//        rendererPage= pdfRenderer?.openPage(count)!!
//        val rendererPageWidth=rendererPage.width
//        val rendererPageHeight=rendererPage.height
//
//        val bitmap: Bitmap = Bitmap.createBitmap(
//            rendererPageWidth,rendererPageHeight, Bitmap.Config.ARGB_8888
//        )
//        rendererPage.render(bitmap,null,null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
//        pdfView.setImageBitmap(bitmap)
//    }
//}