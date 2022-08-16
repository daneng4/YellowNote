package com.hansung.yellownote.drawing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import androidx.viewpager2.widget.ViewPager2


class DrawingView @JvmOverloads constructor(
    context: Context?,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(
    context!!, attr, defStyle
) {
    lateinit var viewPager2 :ViewPager2

    var positionX:Float = 0.0f
    var positionY:Float = 0.0f
    var refX:Float = 0.0f
    var refY:Float = 0.0f
    var mScaleGestureDetector:ScaleGestureDetector? = null
    var scaleFactor = 1.0f
    val mMinZoom = 1.0f
    val mMaxZoom = 3.0f

    lateinit var pdfReader: PdfReader
    lateinit var path: Path
    lateinit var backgroundPaint: Paint
    lateinit var drawingPaint: Paint
    lateinit var drawingBitmap: Bitmap
    lateinit var backgroundBitmap: Bitmap
    lateinit var canvas: Canvas
//    lateinit var drawingCanvas: Canvas
    lateinit var EditImagematrix: Matrix
    var pageInfo: PageInfo? = null
    lateinit var customPath:CustomPath
    var penColor = Color.BLACK
    var penThickness = 10F
    var drawingMode = "pen"


    init{
        System.out.println("***************DrawingView 생성*****************") //2
//        customPaths = ArrayList<CustomPath>()
//        paths = ArrayList<Path>()

//        mScaleGestureDetector= ScaleGestureDetector(getContext(),ScaleListener())
    }

    @SuppressLint("ClickableViewAccessibility")
    fun createDrawingBitmap(backgroundBitmap:Bitmap){
        this.backgroundBitmap = backgroundBitmap
//        System.out.println("backgroundBitmap = ${backgroundBitmap.width}x${backgroundBitmap.height}")
        drawingBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, backgroundBitmap.config)
        setupDrawing()
    }

    fun setupDrawing(){
        canvas = Canvas(drawingBitmap)
//        drawingCanvas
//        path = Path()
        path = Path()
        backgroundPaint = Paint(Paint.DITHER_FLAG)
        setPenStyle(penColor, penThickness)
        drawingPaint.setAntiAlias(true) // 가장자리 표면 매끄럽게

        EditImagematrix = Matrix()
//        canvas.drawBitmap(backgroundBitmap, EditImagematrix, drawingPaint)

        this.setImageBitmap(drawingBitmap)
        setupDrawingView()
    }

    fun setPenStyle(color: Int, thickness:Float){
        drawingPaint = Paint().apply{
            this.color = color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = thickness
        }
//        System.out.println("paint.color = ${drawingPaint.color}")
    }

    override fun refreshDrawableState() {
        super.refreshDrawableState()
//        System.out.println("${this}")
//        System.out.println(pdfReader.penColor)
        this.penColor = pdfReader.penColor
        drawingPaint.color = this.penColor
//        if(pdfReader.pageInfo.drawingView.penColor != null)
//            this.penColor = pdfReader.pageInfo.drawingView.penColor
        pdfReader.drawingView = this
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas != null) {
            canvas.save()
//            canvas.translate(positionX,positionY)
//            canvas.scale(scaleFactor,scaleFactor)
            canvas.drawBitmap(backgroundBitmap,EditImagematrix,backgroundPaint)
//            canvas.drawPath(path,drawingPaint)
            canvas.drawBitmap(drawingBitmap,EditImagematrix,backgroundPaint)
            canvas.restore()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupDrawingView(){
        mScaleGestureDetector= ScaleGestureDetector(context,ScaleListener())

        // DrawingView의 touchListener 설정
        this.setOnTouchListener { view, motionEvent ->
            val toolType=motionEvent?.getToolType(0)

            when(toolType) { // 손가락(슬라이드, 확대.축소)
                MotionEvent.TOOL_TYPE_FINGER-> {
                    viewPager2.isUserInputEnabled = true // 페이지 슬라이드 활성화
                    mScaleGestureDetector?.onTouchEvent(motionEvent)
                    when(motionEvent.action){
                        MotionEvent.ACTION_DOWN->{
//                            System.out.println("${motionEvent.x},${motionEvent.y}")
                            refX = motionEvent.x
                            refY = motionEvent.y
                        }
                        MotionEvent.ACTION_MOVE->{
//                            System.out.println("${motionEvent.x},${motionEvent.y}")
                            var nX = motionEvent.x
                            var nY = motionEvent.y

                            positionX += nX-refX
                            positionY += nY-refY

                            refX = nX
                            refY = nY

                            invalidate()
                        }
                    }
                }
                else-> { // 펜(필기)
                    val x = motionEvent.x
                    val y = motionEvent.y
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
//                            System.out.println("현재 drawingView ==> ${this}")
//                            drawingPaint.color = Color.RED
//                            System.out.println("$drawingPaint.color")
                            viewPager2.isUserInputEnabled = false // 페이지 슬라이드 비활성화
                            path.reset()
                            path.moveTo(x, y)
                            customPath = CustomPath(PointF(x,y),penColor,penThickness)
                            canvas.drawPath(path,drawingPaint)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            path.lineTo(x, y)
                            customPath.addPoint(PointF(x,y))
                            canvas.drawPath(path,drawingPaint)
                        }
                        MotionEvent.ACTION_UP -> {
                            path.lineTo(x, y)
                            customPath.endPoint = PointF(x,y)
                            pageInfo?.customPaths?.add(customPath)
                            canvas.drawPath(path, drawingPaint)
                            path.reset()
                        }
                    }
                    this.invalidate()
                }
            }
            true
        }
    }

    fun setErase(){
        System.out.println("eraser 모드로 변경")
        drawingPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
    }

    fun changePageInfo(pageInfo: PageInfo){
        this.pageInfo = pageInfo
//        System.out.println("${this} pageInfo의 page번호 = ${pageInfo.pageNo}")
        pdfReader.pageInfo.drawingView = this
//        System.out.println("DrawingInfo의 pageNo ${this.pageInfo!!.pageNo}로 변경")
    }

//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        drawingPaint.color = penColor
//        canvas.drawPath(path,drawingPaint)
//    }


//    inner class ScaleListener: ScaleGestureDetector.SimpleOnScaleGestureListener(){
//        override fun onScale(detector: ScaleGestureDetector): Boolean {
//            viewPager2.isUserInputEnabled = false // 페이지 슬라이드 활성화
//            scaleFactor*=detector.scaleFactor
//            scaleFactor=Math.max(0.5f,Math.min(scaleFactor,2.5f))
////            println("scaleFactor= $scaleFactor")
//            this@DrawingView.scaleX=scaleFactor
//            this@DrawingView.scaleY=scaleFactor
//            return true
//        }
//    }

    inner class ScaleListener: ScaleGestureDetector.SimpleOnScaleGestureListener(){
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            viewPager2.isUserInputEnabled = false // 페이지 슬라이드 활성화
            scaleFactor*=detector.scaleFactor
//            if(scaleFactor<mMaxZoom)
//                scaleFactor = mMinZoom/scaleFactor
//            else if(scaleFactor>mMaxZoom)
//                scaleFactor = mMaxZoom/scaleFactor
            scaleFactor=Math.max(1.0F,Math.min(scaleFactor,mMaxZoom))
//            println("scaleFactor= $scaleFactor")
            this@DrawingView.scaleX=scaleFactor
            this@DrawingView.scaleY=scaleFactor
//            this@DrawingView.translationX += positionX
//            this@DrawingView.translationY += positionY
            return true
        }
    }
}