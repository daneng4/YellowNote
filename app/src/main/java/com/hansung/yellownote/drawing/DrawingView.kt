package com.hansung.yellownote.drawing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Environment
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.appcompat.widget.AppCompatImageView
import androidx.viewpager2.widget.ViewPager2
import com.hansung.yellownote.MqttAdapter
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.MqttException
import java.io.File
import java.io.FileOutputStream


class DrawingView @JvmOverloads constructor(
    context: Context?,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(
    context!!, attr, defStyle
) {
    lateinit var viewPager2 :ViewPager2
    var PageMode = -1 // page mode (그림,확대/축소,이동)
    val NONE = -1
    val DRAWING = 0
    val ZOOM = 1
    val DRAG = 2
    var isZoomed = false // 확대된 상태인지

    var defaultX:Float = 0.0f
    var defaultY:Float = 0.0f
    var oldX:Float = 0.0f
    var oldY:Float = 0.0f
    var mScaleGestureDetector:ScaleGestureDetector? = null
    var mScaleFactor = 1.0f
    val mMinZoom = 1.0f
    val mMaxZoom = 3.0f

    lateinit var pdfReader: PdfReader
    lateinit var path: Path
    lateinit var backgroundPaint: Paint
    lateinit var drawingPaint: Paint
    lateinit var drawingBitmap: Bitmap
    lateinit var backgroundBitmap: Bitmap
    lateinit var canvas: Canvas
    lateinit var EditImagematrix: Matrix
    lateinit var customPath:CustomPath

    // 선택 영역(사각형)의
    var clippingStartPoint: PointF? = null // 사용자 선택 시작점
    var clippingEndPoint: PointF? = null // 사용자 선택 끝점
    var minX = 0f // 좌상단 x좌표
    var minY = 0f // 좌상단 y좌표
    var maxX = 0f // 우하단 x좌표
    var maxY = 0f // 우하단 y좌표
    var selectedPaths:ArrayList<CustomPath> = ArrayList<CustomPath>()

    var pageInfo: PageInfo? = null

    val PEN = 0
    val ERASER = 1
    val TEXT = 2
    val CLIPPING = 3
    val SHAPE = 4
    val MOVING = 5
    var drawingMode = PEN

    init{
        System.out.println("***************${this} 생성*****************") //2
        System.out.println("")
        PageMode = NONE

//        mScaleGestureDetector= ScaleGestureDetector(getContext(),ScaleListener())
    }

    @SuppressLint("ClickableViewAccessibility")
    fun createDrawingBitmap(backgroundBitmap:Bitmap){
        this.backgroundBitmap = backgroundBitmap
//        System.out.println("backgroundBitmap = ${backgroundBitmap.width}x${backgroundBitmap.height}")
        drawingBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, backgroundBitmap.config)
        setupDrawing()
        getViewTreeObserver ().addOnGlobalLayoutListener(object :
            OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                defaultX = this@DrawingView.left.toFloat()
                defaultY = this@DrawingView.top.toFloat()
//                System.out.println("Default Position = ${defaultX},${defaultY}")
//                System.out.println("Default Position = ${this@DrawingView.x},${this@DrawingView.y}")
            }
        })

        
    }

    fun setupDrawing(){
        System.out.println("++++++++++++++++++++++SetupDrawing++++++++++++++++++")
        canvas = Canvas(drawingBitmap)
        path = Path()
        backgroundPaint = Paint(Paint.DITHER_FLAG)
        setPenStyle()
        drawingPaint.setAntiAlias(true) // 가장자리 표면 매끄럽게

        EditImagematrix = Matrix()
        canvas.drawBitmap(backgroundBitmap, EditImagematrix, drawingPaint)

        this.setImageBitmap(drawingBitmap)
        setupDrawingView()

//        this.setImageBitmap(backgroundBitmap)
    }

    fun setPenStyle(){
        System.out.println("******************* setPenStyle ${pageInfo?.drawingView} ***************")
        if(pageInfo?.drawingView == null){
            drawingPaint = Paint().apply{
                this.color = pdfReader.pen.color
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = pdfReader.pen.thickness
            }
        }
        else{
            pageInfo?.drawingView!!.drawingPaint = Paint().apply{
                this.color = pdfReader.pen.color
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = pdfReader.pen.thickness
            }
        }

//        System.out.println("paint.color = ${drawingPaint.color}")
    }

    fun setClippingPen(){
        val dashPath = DashPathEffect(floatArrayOf(5f, 25f), 2F)

        drawingPaint = Paint().apply{
            this.color = Color.GRAY
            style = Paint.Style.STROKE
            pathEffect = dashPath
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 5F
        }
//        System.out.println("paint.color = ${drawingPaint.color}")
    }

    override fun refreshDrawableState() { // 다른 페이지로 이동 시 불림
        super.refreshDrawableState()

        pdfReader.drawingView = this // pdfReader의 drawingView 변경

        System.out.println("*****************************************************")
        System.out.println("refreshDrawableState ${pdfReader.drawingView}")
        System.out.println("*****************************************************")
//        drawingPaint.color = pdfReader.pen.color

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas != null) {
            canvas.save()
            canvas.drawBitmap(backgroundBitmap,EditImagematrix,backgroundPaint) // backgroundBitmap 그리기
            canvas.drawBitmap(drawingBitmap,EditImagematrix,backgroundPaint) // drawingBitmap 그리기
            if(drawingMode==CLIPPING)
                canvas.drawRect(clippingStartPoint!!.x,clippingStartPoint!!.y,clippingEndPoint!!.x,clippingEndPoint!!.y,drawingPaint)
            canvas.restore()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupDrawingView(){
        mScaleGestureDetector= ScaleGestureDetector(context,ScaleListener())
        viewPager2.isUserInputEnabled = false // 페이지 넘기기 비활성화

        // DrawingView의 touchListener 설정
        this.setOnTouchListener { view, motionEvent ->
            val toolType=motionEvent?.getToolType(0)

            when(toolType) { // 손가락(슬라이드, 확대.축소, 페이지 내 이동)
                MotionEvent.TOOL_TYPE_FINGER-> {
                    if(isZoomed == false) // 확대 안되어 있는 경우
                        viewPager2.isUserInputEnabled = true // 페이지 넘기기 활성화
                    mScaleGestureDetector?.onTouchEvent(motionEvent)

                    when(motionEvent.action){
                        MotionEvent.ACTION_DOWN->{
                            if(isZoomed) {
                                PageMode = DRAG
                                viewPager2.isUserInputEnabled = false
                                oldY = motionEvent.y
                                oldX = motionEvent.x
                            }
                        }
                        MotionEvent.ACTION_MOVE->{
//                            System.out.println("${motionEvent.x},${motionEvent.y}")
                            if(isZoomed && PageMode==DRAG){
                                // 확대된 상태인 경우 페이지 내에서 위치 이동
                                var newX = motionEvent.x
                                var newY = motionEvent.y

                                this.x += newX - oldX
                                this.y += newY - oldY

                                oldX = newX
                                oldY = newY

                                invalidate()
                            }
                        }
                        MotionEvent.ACTION_POINTER_UP -> { // 손가락 2개
                            System.out.println("***************** ACTION_POINTER_UP *****************")
                            PageMode = NONE
                        }
                    }
                }
                else-> { // 필기 모드 (S펜 사용 시)
                    val x = motionEvent.x
                    val y = motionEvent.y
                    when(drawingMode){
                        CLIPPING -> { // 영역 선택 모드 (사각형으로)
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    System.out.println("현재 drawingView ==> ${this}")
                                    viewPager2.isUserInputEnabled = false // 페이지 넘기기 비활성화
                                    PageMode = DRAWING // mode 변경

                                    if(selectedPaths.size!=0 && checkContainPoint(PointF(x,y))){
                                        drawingMode = MOVING
                                    }
                                    else{
                                        selectedPaths.clear()
                                        clippingStartPoint = PointF(motionEvent.x,motionEvent.y)
                                        clippingEndPoint = PointF(motionEvent.x,motionEvent.y)
                                    }
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    clippingEndPoint = PointF(motionEvent.x,motionEvent.y)
                                }
                                MotionEvent.ACTION_UP -> {
                                    checkContainSelectedPath()
                                    if(selectedPaths.size==0)
                                        clippingEndPoint = clippingStartPoint
                                    else
                                        saveCanvas()
                                    PageMode = NONE
                                }
                            }
                        }
                        PEN -> { // 펜 모드
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {
//                                    System.out.println("현재 drawingView ==> ${this}")
                                    viewPager2.isUserInputEnabled = false // 페이지 넘기기 비활성화
                                    PageMode = DRAWING // mode 변경

//                                    path.reset()
                                    path = Path()
                                    path.moveTo(x, y)
                                    customPath = CustomPath(PointF(x,y),pdfReader.pen.color,pdfReader.pen.thickness) // path 시작점, 색, 굵기 저장
                                    canvas.drawPath(path,drawingPaint)
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    path.lineTo(x, y)
                                    customPath.addPoint(PointF(x,y))
                                    canvas.drawPath(path,drawingPaint)
                                    System.out.println("${path.isEmpty}")
                                }
                                MotionEvent.ACTION_UP -> {
                                    path.lineTo(x, y)
                                    customPath.endPoint = PointF(x,y) // path 끝점 저장
                                    customPath.path = path
                                    System.out.println("${path.isEmpty}")
                                    customPath.drawingPaint = drawingPaint
                                    pageInfo?.customPaths?.add(customPath) // pageInfo에 customPath 저장
                                    canvas.drawPath(path, drawingPaint)
//                                    path.reset()
                                    System.out.println("${customPath.path.isEmpty}")
                                    System.out.println("path 개수 = ${pageInfo?.customPaths?.size}")
                                }
                            }
                        }
                        ERASER -> { // 지우개 모드
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    System.out.println("현재 drawingView ==> ${this}")
                                    viewPager2.isUserInputEnabled = false // 페이지 넘기기 비활성화
                                    PageMode = DRAWING // mode 변경

                                    path.reset()
                                    path.moveTo(x, y)
                                    canvas.drawPath(path,drawingPaint)
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    path.lineTo(x, y)
                                    canvas.drawPath(path,drawingPaint)
                                }
                                MotionEvent.ACTION_UP -> {
                                    path.lineTo(x, y)
                                    canvas.drawPath(path, drawingPaint)
                                    path.reset()
                                }
                            }
                        }
                        MOVING -> {
                            when (motionEvent.action) {
                                MotionEvent.ACTION_MOVE -> {
                                    System.out.println("MOVING")
//                                    clippingEndPoint = PointF(motionEvent.x,motionEvent.y)
                                }
                                MotionEvent.ACTION_UP -> {
//                                    checkContainSelectedPath()
//                                    if(selectedPaths.size==0)
//                                        clippingEndPoint = clippingStartPoint
//                                    else
//                                        saveCanvas()
                                }
                            }
                        }
                    }
                    this.invalidate()
                }
            }
            true
        }
    }

    private fun checkContainSelectedPath(){ // 선택 영역 내에 포함된 path 찾기
        if(clippingStartPoint!!.x<clippingEndPoint!!.x){
            minX = clippingStartPoint!!.x
            maxX = clippingEndPoint!!.x
        }
        else{
            minX = clippingEndPoint!!.x
            maxX = clippingStartPoint!!.x
        }

        if(clippingStartPoint!!.y<clippingEndPoint!!.y){
            minY = clippingStartPoint!!.y
            maxY = clippingEndPoint!!.y
        }
        else{
            minY = clippingEndPoint!!.y
            maxY = clippingStartPoint!!.y
        }

        // customPaths 중 사각형 내에 포함되는 customPath 찾기
        for(i in 0..pageInfo!!.customPaths.size-1){
            var checkPath = pageInfo!!.customPaths[i]
            for(j in 0..checkPath.points.size-1){
                var pointF = checkPath.points[j]

                if(checkContainPoint(pointF)){
                    selectedPaths.add(checkPath)
                    break
                }
            }
        }

        System.out.println("selectedPaths개수 = ${selectedPaths.size}")
    }

    // 사각형 안에 point 포함되는지 판단
    private fun checkContainPoint(point:PointF):Boolean{
        return (point.x in minX..maxX && point.y in minY..maxY)
    }

    private fun saveCanvas(){
//        val selectBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, Bitmap.Config.ARGB_8888)
//        drawingBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, Bitmap.Config.ARGB_8888)
////        canvas.setBitmap(drawingBitmap)
//        var selectedCanvas = Canvas(drawingBitmap)
//        selectedCanvas.setBitmap(drawingBitmap)
////        this.setImageBitmap(drawingBitmap)
//        drawingPaint.color = Color.BLUE
//
//        for(i in 0..selectedPaths.size-1) {
//            selectedCanvas.drawPath(selectedPaths[i].path, selectedPaths[i].drawingPaint)
//            System.out.println("${selectedPaths[i]},${selectedPaths[i].path.isEmpty}")
//        }
//        invalidate()

////////////////////////
        drawingBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, Bitmap.Config.ARGB_8888)
        var selectedCanvas = Canvas(drawingBitmap)
        selectedCanvas.setBitmap(drawingBitmap)
        drawingPaint.color = Color.RED

        for(i in 0..selectedPaths.size-1) {
            selectedCanvas.drawPath(selectedPaths[i].path, selectedPaths[i].drawingPaint)
            System.out.println("${selectedPaths[i]},${selectedPaths[i].path.isEmpty}")
        }

        invalidate()

        val drawings = IntArray(backgroundBitmap.width * backgroundBitmap.height)
        drawingBitmap.getPixels(drawings,0,backgroundBitmap.width,0,0,backgroundBitmap.width,backgroundBitmap.height)


        val returnPixels = ByteArray(drawings.size)
//
        for(i in drawings.indices){
            val pix = drawings[i]
            val b = pix and 0xff
            returnPixels[i]=(b/255.0).toInt().toByte()
        }
        pdfReader.client.sendPixelMessage(returnPixels)
//
//        var i=0
//        for (y in drawings) {
//            if(i%backgroundBitmap.width==0)System.out.println()
//                System.out.print("$y ")
//                i++
//        }
//        for (y in 0..backgroundBitmap.height-1) {
//            for (x in 0..backgroundBitmap.width-1) {
//                drawings[y * backgroundBitmap.width + x] = b.getPixel(x, y)
//                System.out.println("BitmapView X=$x,Y=$y")
//            }
//        }

// val path = Environment.getExternalStorageDirectory().absolutePath
//        try{
//            drawingBitmap.compress(Bitmap.CompressFormat.JPEG,100,FileOutputStream(File(Environment.getExternalStorageDirectory().absolutePath+"/selectedPath.jpg")))
//        }catch (e:Exception){
//            System.out.println("testSaveView, Exception: $e")
//        }
//        if (drawingBitmap != null) {
//            try {
//                val f = File("$path/notes")
//                f.mkdir()
//                val f2 = File("$path/notes/selectedPath.png")
//                val fos = FileOutputStream(f2)
//                if (fos != null) {
//                    drawingBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
//                    fos.close()
//                }
//
//                //setWallpaper( b );
//            } catch (e: Exception) {
//                System.out.println("testSaveView, Exception: $e")
//            }
//        }
    }

//    fun setErase(){
//        System.out.println("eraser 모드로 변경")
//        drawingPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
//    }


    fun changePageInfo(pageInfo: PageInfo){
        this.pageInfo = pageInfo
        pageInfo.drawingView = this
        System.out.println("2. ${pageInfo.pageNo}의 drawingView ${pageInfo.drawingView}로 변경")
    }

    inner class ScaleListener: ScaleGestureDetector.SimpleOnScaleGestureListener(){
        private val SPAN_SLOP = 6

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            super.onScaleBegin(detector)
            PageMode = ZOOM
            System.out.println("PageMode = ZOOM")
//            viewPager2.isUserInputEnabled = false // 페이지 넘기기 비활성화

            // 확대/축소되는 중심점 변경
            val newX = detector.focusX
            val newY = detector.focusY
            translationX = translationX + (pivotX - newX) * (1 - scaleX)
            translationY = translationY + (pivotY - newY) * (1 - scaleY)
            pivotX = newX
            pivotY = newY

            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if(gestureTolerance(detector)){
                if(detector.timeDelta>10 && detector.timeDelta<20){
                    System.out.println("${detector.timeDelta}")

                    mScaleFactor*=detector.scaleFactor
                    mScaleFactor=Math.max(mMinZoom,Math.min(mScaleFactor,mMaxZoom))

                    this@DrawingView.scaleX=mScaleFactor
                    this@DrawingView.scaleY=mScaleFactor

                    isZoomed = mScaleFactor != 1.0F
                    if(mScaleFactor == 1.0F){
                        this@DrawingView.x = defaultX
                        this@DrawingView.y = defaultY
                    }
                }
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            PageMode = NONE
//            viewPager2.isUserInputEnabled = true // 페이지 넘기기 활성화
        }

        private fun gestureTolerance(detector: ScaleGestureDetector): Boolean {
            val spanDelta = Math.abs(detector.currentSpan - detector.previousSpan)
            System.out.println("spanDelta = ${spanDelta}")
            return spanDelta > SPAN_SLOP && spanDelta < 25
        }
    }
}