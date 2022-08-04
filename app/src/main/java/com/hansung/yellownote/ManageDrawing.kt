package com.hansung.yellownote

import android.annotation.SuppressLint
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.toColor
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_pdf.*
import java.util.*

class ManageDrawing(filePath:String, pdf_view_pager:ViewPager2) {
    var filePath:String
    var page = 0
    var customPaths: ArrayList<CustomPath>
    lateinit var customPath: CustomPath
    lateinit var path: Path
    lateinit var paint: Paint
    lateinit var drawingBitmap: Bitmap
    lateinit var pdfBitmap: Bitmap
    lateinit var canvas: Canvas
    lateinit var EditImagematrix: Matrix
    lateinit var mode:String
    var color:Int = 0

    lateinit var pdfView: ImageView
    var width = 0
    var height = 0

    var pdf_view_pager : ViewPager2

    var paths : ArrayList<Path>

    init{
        this.filePath = filePath
        this.pdf_view_pager = pdf_view_pager
        customPaths = ArrayList<CustomPath>()
        paths = ArrayList<Path>()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun createDrawingBitmap(width:Int, height:Int, pdfView: ImageView){
        this.width = width
        this.height = height
        this.pdfView = pdfView

        drawingBitmap = Bitmap.createBitmap(width, height, pdfBitmap.config)
        setupDrawing()

        pdfView.setOnTouchListener { _: View, event:MotionEvent ->
            val toolType=event.getToolType(0)

            when(toolType) {
                MotionEvent.TOOL_TYPE_FINGER-> {
//                    System.out.println("손가락")
                    pdf_view_pager.isUserInputEnabled = true // 페이지 슬라이드 허용
                }
                else-> {
                    val x = event.x
                    val y = event.y
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            pdf_view_pager.isUserInputEnabled = false // 페이지 슬라이드 금지
                            System.out.println("${page}쪽 펜 색 = ${color.toColor()}")
//                            System.out.println("펜")
//                            System.out.println("StartPoint = ("+x+","+y+")")
                            customPath = CustomPath(Point(x.toInt(),y.toInt()), Color.RED, 10)
                            customPaths.add(customPath)
                            path.reset()
                            path.moveTo(x, y)
                            canvas.drawPath(path, paint)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            customPath.addPoint(Point(x.toInt(),y.toInt()))
                            path.lineTo(x, y)
                            canvas.drawPath(path, paint)
                        }
                        MotionEvent.ACTION_UP -> {
                            customPath.endPoint = Point(x.toInt(),y.toInt())
                            path.lineTo(x, y)
                            canvas.drawPath(path, paint)
                            path.reset()
//                            System.out.println("펜")
//                            System.out.println("EndPoint = ("+x+","+y+")")
                        }
                    }
                    pdfView.invalidate()
                }
            }
            true
        }
    }

    fun changePenColor(color:Int){
        System.out.println("changePenColor")
        this.color = color
        paint.color = color
    }

    fun setupDrawing() {
        canvas = Canvas(drawingBitmap)
        path = Path()
        paint = Paint().apply{
            color = this.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 10F
        }
        paint.setAntiAlias(true) // 가장자리 표면 매끄럽게

        EditImagematrix = Matrix()
        canvas.drawBitmap(pdfBitmap, EditImagematrix, paint)

        pdfView.setImageBitmap(drawingBitmap)
//        System.out.println("pdfView = ${pdfView.width},${pdfView.height}")
//        System.out.println("drawingBitmap = ${drawingBitmap.width}x${drawingBitmap.height}")
//        System.out.println("pdfBitmap = ${pdfBitmap.width}x${pdfBitmap.height}")
//        System.out.println("aa = ${pdfView.})
    }
}