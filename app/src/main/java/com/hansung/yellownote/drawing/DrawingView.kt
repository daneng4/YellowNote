package com.hansung.yellownote.drawing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.PopupMenu
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.hansung.yellownote.R
import java.lang.Exception


class DrawingView @JvmOverloads constructor(
    context: Context?,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(
    context!!, attr, defStyle
) {
    lateinit var pdfActivity:PdfActivity
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
    val dashPath = DashPathEffect(floatArrayOf(5f, 25f), 2F)

    // 선택 영역(사각형)의
    var clippingPaint : Paint
    var clippingStartPoint: PointF? = null // 사용자 선택 시작점
    var clippingEndPoint: PointF? = null // 사용자 선택 끝점
    var pathOldX = 0f // path 위치 이동 시 이용
    var pathOldY = 0f // path 위치 이동 시 이용
    var offsetX = 0f
    var offsetY = 0f
    var selectedPaths:ArrayList<CustomPath> = ArrayList<CustomPath>()

    var pageInfo: PageInfo? = null

    val PEN = 0
    val ERASER = 1
    val TEXT = 2
    val CLIPPING = 3
    val SHAPE = 4
    var oldDrawingMode = NONE

    lateinit var penInfo : PenInfo

    init{
        pdfActivity = this.context as PdfActivity
        PageMode = DRAWING
        penInfo = pdfActivity.penInfo

        penInfo.PenMode.observe(pdfActivity){
            System.out.println("${penInfo.getPenMode()}, ${penInfo.getMovingClipping()}")
            if(penInfo.getMovingClipping()){
                System.out.println("다시 그리기")
                if(pageInfo!!.customPaths.size>0){
                    for(i in 0..pageInfo!!.customPaths.size-1) {
                        var customPath = pageInfo!!.customPaths[i]
                        canvas.drawPath(customPath.path, customPath.drawingPaint)
                    }
                }
                invalidate()
            }
        }

        clippingPaint = Paint().apply{
            this.color = penInfo.getPenColor()!!
            style = Paint.Style.STROKE
            pathEffect = dashPath
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 5F
        }
//        var popUpMenu = PopupMenu(this.context,this)
//        popUpMenu.menuInflater.inflate(R.menu.menu_selectpath)
//        mScaleGestureDetector= ScaleGestureDetector(getContext(),ScaleListener())
    }

    @SuppressLint("ClickableViewAccessibility")
    fun createDrawingBitmap(backgroundBitmap:Bitmap){
        this.backgroundBitmap = backgroundBitmap
        drawingBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, backgroundBitmap.config)
        setupDrawing()
        viewTreeObserver.addOnGlobalLayoutListener(object :
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
        when(penInfo.getPenMode()){
            PEN -> setDrawingPen()
            CLIPPING -> setClippingPen()
            ERASER -> setEraser()
        }
    }

    // 일반펜 설정
    fun setDrawingPen(){
        drawingPaint = Paint().apply{
            this.color = penInfo.getPenColor()!!
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = penInfo.getPenWidth()
            System.out.println("drawingview pen 색 설정 ${this.color}")
        }
    }

    // 클리핑 펜 설정
    fun setClippingPen(){
        drawingPaint = Paint().apply{
            this.color = penInfo.getPenColor()!!
            style = Paint.Style.STROKE
            pathEffect = dashPath
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 5F
        }
    }

    // 지우개 설정
    fun setEraser(){
        drawingPaint.strokeWidth = penInfo.getPenWidth()
        drawingPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
    }

    // 다른 페이지로 이동 시 불림
    override fun refreshDrawableState() {
        super.refreshDrawableState()

        pdfReader.drawingView = this // pdfReader의 drawingView 변경
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas != null) {
            canvas.drawBitmap(backgroundBitmap,EditImagematrix,backgroundPaint) // backgroundBitmap 그리기
            canvas.drawBitmap(drawingBitmap,EditImagematrix,backgroundPaint) // drawingBitmap 그리기

            // CLIPPING 모드인 경우 사각형 모양의 그물 그리기
            if(penInfo.getPenMode()==CLIPPING){
                try{
                    canvas.drawRect(clippingStartPoint!!.x,clippingStartPoint!!.y,clippingEndPoint!!.x,clippingEndPoint!!.y,drawingPaint)
                    if(penInfo.getMovingClipping()){
                        for(i in 0..selectedPaths.size-1) {
                            var selectedCustomPath = selectedPaths[i]
                            selectedCustomPath.path.offset(offsetX, offsetY) // path 위치 변경
                            selectedCustomPath.changePointPosition(offsetX,offsetY) // customPath에 저장된 point들 위치 변경
                            canvas.drawPath(selectedCustomPath.path, selectedCustomPath.drawingPaint) // 이동한 path 다시 그리기
                        }
                    }
                }catch (e:Exception){
                    System.out.println("다음 페이지")
                }
            }
        }
    }


    fun redrawPath(exceptSeletedPath:Boolean){
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawingBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, backgroundBitmap.config)
        canvas = Canvas(drawingBitmap)
        EditImagematrix = Matrix()
        canvas.drawBitmap(backgroundBitmap, EditImagematrix, drawingPaint)
        this.setImageBitmap(drawingBitmap)

        if(exceptSeletedPath){
            for (i in 0..pageInfo!!.customPaths.size - 1) {
                var customPath = pageInfo!!.customPaths[i]
                if (selectedPaths.contains(customPath))
                    continue
                canvas.drawPath(customPath.path, customPath.drawingPaint)
            }
        }
        else{
            for (i in 0..pageInfo!!.customPaths.size - 1) {
                var customPath = pageInfo!!.customPaths[i]
                if(selectedPaths.contains(customPath)){
                    customPath.path.offset(offsetX, offsetY)
                    customPath.changePointPosition(offsetX,offsetY)
                }
                canvas.drawPath(customPath.path, customPath.drawingPaint)
            }
        }
    }

    fun showMenu(){
        var popupMenu = PopupMenu(context, this)
        popupMenu.menuInflater.inflate(R.menu.popup_menu,popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener {
            when(it.itemId){
                R.id.delete-> {
                    System.out.println("delete")
                }
                R.id.changeColor -> {
                    System.out.println("changeColor")
                }
                R.id.handToText -> {
                    System.out.println("handToText")
                }
            }
            true
        })
        popupMenu.dragToOpenListener
        popupMenu.show()
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
                    when(penInfo.getPenMode()){
                        CLIPPING -> { // 영역 선택 모드 (사각형으로)
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    setClippingPen()
                                    System.out.println("현재 drawingView ==> ${this}")
                                    viewPager2.isUserInputEnabled = false // 페이지 넘기기 비활성화
                                    PageMode = DRAWING // mode 변경

                                    if(selectedPaths.size!=0 && checkContainPoint(PointF(x,y))){ // 선택한 영역 내부 터치한 경우
                                        // 선택된 path들을 이동시키는 모드로 변경 (MOVING MODE)
                                        penInfo.setMovingClipping(true)
                                        System.out.println("${penInfo.getPenMode()}, ${penInfo.getMovingClipping()}")
                                        pathOldX = x
                                        pathOldY = y
                                        redrawPath(true) // selectedPaths 제외한 path들 다시 그리기
                                    }
                                    else{
                                        if(penInfo.getMovingClipping()){
                                            penInfo.setMovingClipping(false)
                                            for(i in 0..pageInfo!!.customPaths.size-1) {
                                                var customPath = pageInfo!!.customPaths[i]
                                                canvas.drawPath(customPath.path, customPath.drawingPaint)
                                            }
                                        }
                                        selectedPaths.clear()
                                        clippingStartPoint = PointF(motionEvent.x,motionEvent.y)
                                        clippingEndPoint = PointF(motionEvent.x,motionEvent.y)
                                    }
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    if(penInfo.getMovingClipping()){
                                        // 움직인 정도
                                        offsetX = x - pathOldX
                                        offsetY = y - pathOldY
                                        // 사각형 그물 좌표 변경
                                        clippingStartPoint!!.x += offsetX
                                        clippingStartPoint!!.y += offsetY
                                        clippingEndPoint!!.x += offsetX
                                        clippingEndPoint!!.y += offsetY

                                        pathOldX = x
                                        pathOldY = y
                                    }
                                    else{
                                        clippingEndPoint = PointF(motionEvent.x,motionEvent.y)
                                    }
                                    invalidate()
                                }
                                MotionEvent.ACTION_UP -> {
                                    System.out.println("${penInfo.getPenMode()}, ${penInfo.getMovingClipping()}")
                                    if(penInfo.getMovingClipping()){
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                    else{
                                        checkContainSelectedPath()
                                        if(selectedPaths.size==0)
                                            clippingEndPoint = clippingStartPoint
//                                    else
//                                        showMenu()
//                                    else
//                                        saveCanvas()
                                        PageMode = NONE
                                    }
                                    invalidate()
                                }
                            }
                        }
                        PEN -> { // 펜 모드
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    setDrawingPen()
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
                                }
                                MotionEvent.ACTION_UP -> {
                                    path.lineTo(x, y)
                                    customPath.endPoint = PointF(x,y) // path 끝점 저장
                                    customPath.path = path
                                    customPath.drawingPaint = drawingPaint
                                    pageInfo?.customPaths?.add(customPath) // pageInfo에 customPath 저장
                                    canvas.drawPath(path, drawingPaint)
//                                    System.out.println("path 개수 = ${pageInfo?.customPaths?.size}")
                                }
                            }
                            invalidate()
                        }
                        ERASER -> { // 지우개 모드
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    setEraser()
//                                    System.out.println("현재 drawingView ==> ${this}")
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
                            invalidate()
                        }
//                        MOVING -> {
//                           when(motionEvent.action){
//                               MotionEvent.ACTION_DOWN -> {
//                                   if(selectedPaths.size!=0 && checkContainPoint(PointF(x,y))){ // 선택한 영역 내부 터치한 경우
//                                       pathOldX = x
//                                       pathOldY = y
//                                   }
//                                   else{
//                                       redrawPath(false) // path 순서에 맞춰서 다시 그리기
//                                       penData.setPenMode(NONE)
//                                       oldDrawingMode = MOVING
//                                       clippingStartPoint = PointF(x,y)
//                                   }
//                               }
//                               MotionEvent.ACTION_MOVE -> {
//                                   // 움직인 정도
//                                   offsetX = x - pathOldX
//                                   offsetY = y - pathOldY
//                                   // 사각형 그물 좌표 변경
//                                   clippingStartPoint!!.x += offsetX
//                                   clippingStartPoint!!.y += offsetY
//                                   clippingEndPoint!!.x += offsetX
//                                   clippingEndPoint!!.y += offsetY
//
//                                   invalidate()
//                                   pathOldX = x
//                                   pathOldY = y
//                               }
//                               MotionEvent.ACTION_UP -> {
//                                   offsetX = 0f
//                                   offsetY = 0f
//                               }
//                           }
//                        }
//                        else -> { // drawingMode == NONE
//                            when(oldDrawingMode){
//                                MOVING -> {
//                                    when (motionEvent.action) {
//                                        MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
//                                            penData.setPenMode(CLIPPING)
//                                            selectedPaths.clear()
//                                        }
//                                    }
//                                }
//                            }
//                        }
                    }
                }
            }
            true
        }
    }

    // 선택 영역 내에 포함된 path 찾기
    private fun checkContainSelectedPath(){
        if(clippingStartPoint == null)
            return

        var minX = 0f // 좌상단 x좌표
        var minY = 0f // 좌상단 y좌표
        var maxX = 0f // 우하단 x좌표
        var maxY = 0f // 우하단 y좌표

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

        clippingStartPoint = PointF(minX,minY)
        clippingEndPoint = PointF(maxX,maxY)

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
//        System.out.println("selectedPaths개수 = ${selectedPaths.size}")
    }

    // 사각형 안에 point 포함되는지 판단
    private fun checkContainPoint(point:PointF):Boolean{
        return (point.x in clippingStartPoint!!.x..clippingEndPoint!!.x && point.y in clippingStartPoint!!.y..clippingEndPoint!!.y)
    }

    // mqtt로 선택한 path들 그려서 보내기
    private fun saveCanvas(){
//        val selectBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, Bitmap.Config.ARGB_8888)
//        drawingBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, Bitmap.Config.ARGB_8888)
////        canvas.setBitmap(drawingBitmap)
//        var selectedCanvas = Canvas(drawingBitmap)
//        selectedCanvas.setBitmap(drawingBitmap)
////        this.setImageBitmap(drawingBitmap)
//        drawingPaint.color = Color.BLUE
        drawingBitmap = Bitmap.createScaledBitmap(drawingBitmap, backgroundBitmap.width,
            backgroundBitmap.height, false)
        var selectedCanvas = Canvas(drawingBitmap) // 선택된 path 위한 canvas
        selectedCanvas.setBitmap(drawingBitmap)
        selectedCanvas.drawColor(Color.WHITE)

        for(i in 0..selectedPaths.size-1) {
            selectedCanvas.drawPath(selectedPaths[i].path, selectedPaths[i].drawingPaint)
        }
        invalidate()

        val drawings = IntArray(backgroundBitmap.width * backgroundBitmap.height)
        drawingBitmap.getPixels(drawings,0,drawingBitmap.width,0,0,drawingBitmap.width,drawingBitmap.height)

        val returnPixels = ByteArray(drawings.size)

        for(i in drawings.indices){
            val pix = drawings[i]
            var b=pix and 0xff
            if(b!=0)b=255
            returnPixels[i]=(b/255).toByte()
        }

        /////////////////////////
        pdfReader.client.sendImageSizeMessage(backgroundBitmap.width)
        println(backgroundBitmap.height)
        pdfReader.client.sendPixelMessage(returnPixels)
        //////////////////////////

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
//                //setWallpaper( b );
//            } catch (e: Exception) {
//                System.out.println("testSaveView, Exception: $e")
//            }
//        }
    }

    fun changePageInfo(pageInfo: PageInfo){
        this.pageInfo = pageInfo
        if(pageInfo.customPaths.size>0){
            for(i in 0..pageInfo.customPaths.size-1){
                var customPath = pageInfo.customPaths[i]
                canvas.drawPath(customPath.path, customPath.drawingPaint)
            }
            invalidate()
        }
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
//                    System.out.println("${detector.timeDelta}")

                    mScaleFactor*=detector.scaleFactor
                    mScaleFactor=Math.max(mMinZoom,Math.min(mScaleFactor,mMaxZoom))

                    this@DrawingView.scaleX=mScaleFactor
                    this@DrawingView.scaleY=mScaleFactor

                    isZoomed = (mScaleFactor != 1.0F)
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
//            System.out.println("spanDelta = ${spanDelta}")
            return spanDelta > SPAN_SLOP && spanDelta < 25
        }
    }
}