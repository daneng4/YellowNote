package com.hansung.yellownote.drawing

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.Drawable
import android.text.InputType
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.viewpager2.widget.ViewPager2
import com.hansung.yellownote.R
import com.hansung.yellownote.databinding.ActivityPdfBinding
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.ArrayList
import kotlin.math.pow

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
    var drawingPaint: Paint = Paint()
    lateinit var drawingBitmap: Bitmap
    lateinit var backgroundBitmap: Bitmap
    lateinit var canvas: Canvas
    lateinit var EditImagematrix: Matrix
    var customPath:CustomPath = CustomPath(PointF(0f,0f))
    val dashPath = DashPathEffect(floatArrayOf(5f, 25f), 2F)

    // 선택 영역(사각형)의
    var drawClippingBox = true
    var clippingPaint : Paint = Paint()
    var clippingStartPoint: PointF? = null // 사용자 선택 시작점
    var clippingEndPoint: PointF? = null // 사용자 선택 끝점
    var pathOldX = 0f // path 위치 이동 시 이용
    var pathOldY = 0f // path 위치 이동 시 이용
    var offsetX = 0f
    var offsetY = 0f
    var selectedPaths:ArrayList<CustomPath> = ArrayList<CustomPath>()
    lateinit var popupWindow:PopupWindow
    var popup:View? = null
    //    var clickChangeColorBtn = false
    val editTexts=ArrayList<EditText>()
    var binding:ActivityPdfBinding
    var rootLayout:ConstraintLayout
    var textLayout:ConstraintLayout
    var textButton=false
    var textPointX=0f
    var textPointY=0f

    var eraserPaint:Paint = Paint()
    var eraserCirclePaint:Paint = Paint()
    var eraserCircleRadius = 0f
    var erasedPaths:ArrayList<CustomPath> = ArrayList<CustomPath>()
    var eraserPath:Path = Path()
    lateinit var eraserPoints:CustomPath
    var eraserPointX = -10f
    var eraserPointY = -10f
    val deletePoints=ArrayList<PointF>()

    var pageInfo: PageInfo? = null

    val PEN = 0
    val ERASER = 1
    val TEXT = 2
    val CLIPPING = 3
    var oldDrawingMode = NONE

    lateinit var penInfo : PenInfo
    lateinit var scope:CoroutineScope

    init{
        pdfActivity = this.context as PdfActivity
        pdfActivity.drawingView = this
        PageMode = DRAWING
        penInfo = pdfActivity.penInfo
        binding=pdfActivity.binding
        rootLayout=binding.root
        textLayout= ConstraintLayout(this.context)

        println("DrawingView PageInfo: ${pageInfo}")
        penInfo.PenMode.observe(pdfActivity){
            System.out.println("${penInfo.getPenMode()}, ${penInfo.getMovingClipping()}")
            if(penInfo.getMovingClipping()){
                System.out.println("다시 그리기")
                try{
                    if(pageInfo!!.customPaths.size>0){
                        for(i in 0..pageInfo!!.customPaths.size-1) {
                            var customPath = pageInfo!!.customPaths[i]
                            canvas.drawPath(customPath.path, customPath.drawingPaint)
                        }
                    }
                    invalidate()
                }catch (e:Exception){}
            }
        }
//        mScaleGestureDetector= ScaleGestureDetector(getContext(),ScaleListener())
    }
    @SuppressLint("ClickableViewAccessibility")
    fun setTextLayout(){
        textLayout= ConstraintLayout(context)
        textLayout.id=View.generateViewId()
        val sample=binding.sampleLayout
        textLayout.layoutParams=sample.layoutParams
        rootLayout.addView(textLayout)
        var constraintSet= ConstraintSet()
        constraintSet.clone(textLayout)
        constraintSet.connect(textLayout.id,ConstraintSet.LEFT,
            ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0)
        constraintSet.connect(textLayout.id,ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.TOP,0)
        constraintSet.connect(textLayout.id,ConstraintSet.RIGHT,
            ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,0)
        constraintSet.connect(textLayout.id,ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,0)
        constraintSet.applyTo(textLayout)
        editTexts.clear()
        if(pageInfo?.customEditText!=null) {
            if (pageInfo!!.customEditText.size != 0) {
                val customEditText=pageInfo!!.customEditText
                println("editText empty = ${customEditText.isEmpty()}")
                for (i in 0..customEditText.size-1) {
                    if(customEditText[i].text=="")continue
                    constraintSet= ConstraintSet()

                    val text = SpannableStringBuilder(customEditText[i].text)
                    val point=customEditText[i].textPoint
                    val editText=EditText(context)
                    editText.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT))
                    editText.text=text
                    editText.id=customEditText[i].textId
                    textLayout.addView(editText)
                    editTexts.add(editText)
                    constraintSet.clone(textLayout)
                    constraintSet.connect(editText.id, ConstraintSet.LEFT,
                        textLayout.id, ConstraintSet.LEFT, point.x.toInt())
                    constraintSet.connect(editText.id, ConstraintSet.TOP,
                        textLayout.id, ConstraintSet.TOP, point.y.toInt())

                    println("text = ${text}, pointx = ${point.x}, pointy = ${point.y}, id = ${editText.id}")

                    constraintSet.applyTo(textLayout)
                    editText.setOnFocusChangeListener{_,hasFocus->
                        if(!hasFocus){
                            for(k in 0.. pageInfo!!.customEditText.size-1){
                                if(editTexts[k]==editText){
                                    editTexts[k].text=editText.text
                                    pageInfo!!.customEditText[k].text=editText.text.toString()
                                }
                            }
                            editText.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT))
                        }
                        else{
                            editText.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK))
                        }

                    }
                }
            }
        }
//
        println("edit Text 초기 설정 완료")
        textLayout.setOnTouchListener{_, motionEvent ->
            val toolType=motionEvent?.getToolType(0)
            textButton=false
            when(toolType) {
                MotionEvent.TOOL_TYPE_FINGER->{
//                MotionEvent.TOOL_TYPE_STYLUS-> { // 필기 모드 (S펜 사용 시)
                    val x = motionEvent.x
                    val y = motionEvent.y
                    println(pageInfo)
                    when(penInfo.getPenMode()){
                        TEXT->{ //텍스트 모드
                            textButton=true
                            textPointX=x
                            textPointY=y
                            when (motionEvent.action) {
                                MotionEvent.ACTION_UP -> {
                                    for(text in editTexts){
                                        text.clearFocus()
                                    }
                                    val customEditText=CustomEditText()
                                    val editText=EditText(context)
                                    customEditText.textPoint=(PointF(textPointX,textPointY))
                                    editTexts.add(editText)
//                                    pageInfo!!.textPaths.add(PointF(textPointX,textPointY))
                                    editText.id=View.generateViewId()
                                    customEditText.textId=editText.id
                                    textLayout.addView(editText)
                                    constraintSet=ConstraintSet()
                                    constraintSet.clone(textLayout)
                                    constraintSet.connect(editText.id,ConstraintSet.LEFT,
                                        textLayout.id, ConstraintSet.LEFT, textPointX.toInt())
                                    constraintSet.connect(editText.id,ConstraintSet.TOP,
                                        textLayout.id, ConstraintSet.TOP,textPointY.toInt())

                                    constraintSet.applyTo(textLayout)
                                    pageInfo!!.customEditText.add(customEditText)
                                    editText.setOnFocusChangeListener{_,hasFocus->
                                        if(!hasFocus){
                                            for(k in 0.. pageInfo!!.customEditText.size-1){
                                                if(editTexts[k]==editText){
                                                    editTexts[k].text=editText.text
                                                    pageInfo!!.customEditText[k].text=editText.text.toString()
                                                }
                                            }
                                            editText.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT))
                                        }
                                        else{
                                            editText.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK))
                                        }

                                    }
                                    editText.setOnLongClickListener{view->
                                        editTexts.remove(editText)
                                        for(k in 0.. pageInfo!!.customEditText.size-1){
                                            if(pageInfo!!.customEditText[k].textId==editText.id) {
                                                pageInfo!!.customEditText.removeAt(k)
                                                break
                                            }
                                        }
                                        textLayout.removeView(editText)
                                        true
                                    }
                                }
                            }
                        }
                        else->{
                            for(text in editTexts){
                                text.clearFocus()
                            }
                        }
                    }
                }

            }
            textButton
        }

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

        /////////////////////////회전해서 다시 그리기 되려나?
//        if(pageInfo!!.customPaths.size>0)
//            redrawPath(false)
//        this.setImageBitmap(backgroundBitmap)
    }

    fun setPenStyle(){
        when(penInfo.getPenMode()){
            PEN -> setDrawingPen()
        }
    }

    // 일반펜 설정
    fun setDrawingPen(){
        drawingPaint = Paint().apply{
            color = penInfo.getPenColor()!!
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = penInfo.getPenWidth()
            System.out.println("drawingview pen 색 설정 ${this.color}")
        }
    }

    // 클리핑 펜 설정
    fun setClippingPen(){
        clippingPaint = Paint().apply{
            color = Color.GRAY
            style = Paint.Style.STROKE
            pathEffect = dashPath
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 5F
        }
    }

    // 지우개 설정
    fun setEraser(){
        eraserPaint = Paint().apply{
            style = Paint.Style.STROKE
            strokeWidth = penInfo.getPenWidth()
            color = Color.WHITE
            setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
        }

        eraserCirclePaint = Paint().apply{
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 5F
            color = Color.BLACK
        }
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


            when(penInfo.getPenMode()){
                CLIPPING -> { // CLIPPING 모드인 경우 사각형 모양의 그물 그리기
                    try{
                        if(drawClippingBox)
                            canvas.drawRect(clippingStartPoint!!.x,clippingStartPoint!!.y,clippingEndPoint!!.x,clippingEndPoint!!.y,clippingPaint)
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
                ERASER -> {
                    canvas.drawCircle(eraserPointX, eraserPointY, eraserCircleRadius, eraserCirclePaint)
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
        println("redrawPath PageInfo: ${pageInfo!!}")
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

    // 클리핑 후 뜨는 팝업창
    fun showClippingPopup(){
        popupWindow = PopupWindow(this)
        var layoutInflater = LayoutInflater.from(this.context)
        popup = layoutInflater.inflate(R.layout.clipping_popup,null)

        popupWindow.contentView = popup
        popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow.isTouchable = true

        popup!!.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        setPopupLocation(popupWindow, popup!!.measuredWidth, popup!!.measuredHeight)
        popupWindow.showAsDropDown(this)

        var deleteClippingBtn = popup!!.findViewById<Button>(R.id.deleteClipping)
        var handToTextBtn = popup!!.findViewById<Button>(R.id.handToText)
        var changeColorBtn = popup!!.findViewById<Button>(R.id.changeColor)

        // 삭제 버튼 클릭 시 선택된 path들 삭제
        deleteClippingBtn.setOnClickListener {
            pageInfo!!.customPaths.removeAll(selectedPaths)
            selectedPaths.clear()
            redrawPath(false)

            popupWindow.dismiss()
            popup=null

            drawClippingBox = false
            invalidate()
        }

        // 텍스트로 변환 버튼 클릭 시 선택된 글자 텍스트로 변환
        handToTextBtn.setOnClickListener {
            popupWindow.dismiss() // 기존 팝업창 끄기

            // 영어,한글, 숫자 선택 팝업창 생성
            popup = layoutInflater.inflate(R.layout.clipping_handtotext_pop,null)

            popupWindow.contentView = popup
            popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            popupWindow.isTouchable = true

            popup!!.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            setPopupLocation(popupWindow, popup!!.measuredWidth, popup!!.measuredHeight)

            var englishBtn = popup!!.findViewById<Button>(R.id.englishBtn)
            var hangulBtn = popup!!.findViewById<Button>(R.id.hangulBtn)
            var numberBtn = popup!!.findViewById<Button>(R.id.numberBtn)

            englishBtn.setOnClickListener {
                saveCanvas("English")
                Toast.makeText(pdfActivity, "영어 손글씨 변환 시도", Toast.LENGTH_LONG).show()
                popupWindow.dismiss()
                popup=null
                drawClippingBox = false
                redrawPath(false)
            }
            hangulBtn.setOnClickListener {
                saveCanvas("Hangul")
                Toast.makeText(pdfActivity, "한글 손글씨 변환 시도", Toast.LENGTH_LONG).show()
                popupWindow.dismiss()
                popup=null
                drawClippingBox = false
                redrawPath(false)
            }
            numberBtn.setOnClickListener {
                saveCanvas("Number")
                Toast.makeText(pdfActivity, "숫자 손글씨 변환 시도", Toast.LENGTH_LONG).show()
                popupWindow.dismiss()
                popup=null
                drawClippingBox = false
                redrawPath(false)
            }
        }

        // 색상 버튼 클릭 시 색상 선택 팝업창 뜸
        changeColorBtn.setOnClickListener {
            popupWindow.dismiss() // 기존 팝업창 끄기

            // 색상 선택할 수 있는 팝업창 생성
            popupWindow = PopupWindow(this)
            var layoutInflater = LayoutInflater.from(this.context)
            popup = layoutInflater.inflate(R.layout.clipping_color_popup,null)

            popupWindow.contentView = popup
            popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            popupWindow.isTouchable = true

            popup!!.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            setPopupLocation(popupWindow, popup!!.measuredWidth, popup!!.measuredHeight)

            var redBtn = popup!!.findViewById<ImageButton>(R.id.redBtn)
            var blueBtn = popup!!.findViewById<ImageButton>(R.id.blueBtn)
            var blackBtn = popup!!.findViewById<ImageButton>(R.id.blackBtn)
            var selectColorBtn = popup!!.findViewById<ImageButton>(R.id.selectColorBtn)

            // 색 버튼 클릭 시 색 변경
            redBtn.setOnClickListener {
                changePathColor(Color.RED)
            }
            blueBtn.setOnClickListener {
                changePathColor(Color.BLUE)
            }
            blackBtn.setOnClickListener {
                changePathColor(Color.BLACK)
            }
            // 무지개색 버튼 클릭 시 색상 직접 선택 가능
            selectColorBtn.setOnClickListener {
                ColorPickerDialog.Builder(this.context,android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).apply{
                    setTitle("")
                    setPreferenceName("ColorPickerDialog")
                    setPositiveButton("SELECT",ColorEnvelopeListener(){ colorEnvelope: ColorEnvelope, b: Boolean ->
                        changePathColor(colorEnvelope.color)
                    })
                    setNegativeButton("CANCEL",DialogInterface.OnClickListener(){
                            dialog: DialogInterface?, which: Int ->  dialog!!.dismiss()
                    })
                    attachAlphaSlideBar(false)
                    attachBrightnessSlideBar(true)
                    setBottomSpace(12)
                }.show()
            }
        }
    }

    // 선택된 path 색 변경
    private fun changePathColor(color:Int){
        for(i in 0..selectedPaths.size-1){
            var selectedPath = selectedPaths[i]
            var idx = pageInfo!!.customPaths.indexOf(selectedPath)
            selectedPath.drawingPaint.color = color
            pageInfo!!.customPaths[idx] = selectedPath
            canvas.drawPath(selectedPath.path, selectedPath.drawingPaint)
        }
        invalidate()
    }

    // 팝업창 위치 지정
    private fun setPopupLocation(popupWindow: PopupWindow, popupWidth:Int, popupHeight:Int){
        var x = this.x+clippingEndPoint!!.x-popupWidth
        var y = this.y+pdfReader.view_pager.y+clippingStartPoint!!.y-popupHeight

        if(x<=0)
            x = 0f
        if(y<pdfReader.view_pager.y)
            y = pdfReader.view_pager.y+popupHeight/2

        popupWindow.showAtLocation(this@DrawingView,Gravity.NO_GRAVITY,x.toInt(),y.toInt())
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupDrawingView(){
        mScaleGestureDetector= ScaleGestureDetector(context,ScaleListener())
        viewPager2.isUserInputEnabled = false // 페이지 넘기기 비활성화
        // DrawingView의 touchListener 설정
        this.setOnTouchListener { view, motionEvent ->
            if(popup!=null&&!popup!!.isFocused){
                popupWindow.dismiss()
                popup=null
            }
            val toolType=motionEvent?.getToolType(0)

            when(toolType) { // 손가락(슬라이드, 확대.축소, 페이지 내 이동)
//                MotionEvent.TOOL_TYPE_FINGER-> {
//                    if(isZoomed == false) // 확대 안되어 있는 경우
//                        viewPager2.isUserInputEnabled = true // 페이지 넘기기 활성화
//                    mScaleGestureDetector?.onTouchEvent(motionEvent)
//
//                    when(motionEvent.action){
//                        MotionEvent.ACTION_DOWN->{
//                            if(isZoomed) {
//                                PageMode = DRAG
//                                viewPager2.isUserInputEnabled = false
//                                oldY = motionEvent.y
//                                oldX = motionEvent.x
//                            }
//                        }
//                        MotionEvent.ACTION_MOVE->{
//                            if(isZoomed && PageMode==DRAG){
//                                // 확대된 상태인 경우 페이지 내에서 위치 이동
//                                var newX = motionEvent.x
//                                var newY = motionEvent.y
//
//                                this.x += newX - oldX
//                                this.y += newY - oldY
//
//                                oldX = newX
//                                oldY = newY
//
//                                invalidate()
//                            }
//                        }
//                        MotionEvent.ACTION_POINTER_UP -> { // 손가락 2개
//                            PageMode = NONE
//                        }
//                    }
//                }
//                else-> { // 필기 모드 (S펜 사용 시)
                MotionEvent.TOOL_TYPE_STYLUS-> { // 필기 모드 (S펜 사용 시)
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
                                    drawClippingBox = true

                                    // 클리핑한 영역 내부 터치한 경우
                                    if(selectedPaths.size!=0 && checkContainPoint(PointF(x,y))){
                                        // 선택된 path들을 이동시키는 모드로 변경 (MOVING MODE)
                                        penInfo.setMovingClipping(true)
                                        pathOldX = x
                                        pathOldY = y
                                        redrawPath(true) // selectedPaths 제외한 path들 다시 그리기
                                    }
                                    // 클리핑한 영역 밖을 터치한 경우
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
                                    if(penInfo.getMovingClipping()){
                                        offsetX = 0f
                                        offsetY = 0f
                                        invalidate()
                                    }
                                    else{
                                        if(pageInfo!= null) {
                                            checkContainSelectedPath()
                                            if (selectedPaths.size == 0) {
                                                clippingEndPoint = clippingStartPoint
                                                invalidate()
                                            } else {
                                                showClippingPopup()
                                            }
                                        }
                                        PageMode = NONE

                                    }
                                }
                            }
                        }
                        PEN -> { // 펜 모드
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    if(selectedPaths.size!=0)
                                        selectedPaths.clear()
                                    setDrawingPen()
                                    viewPager2.isUserInputEnabled = false // 페이지 넘기기 비활성화
                                    PageMode = DRAWING // mode 변경

//                                    path.reset()
                                    path = Path()
                                    path.moveTo(x, y)
                                    customPath = CustomPath(PointF(x,y)) // path 시작점 저장
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
                                    customPath.savePaint(drawingPaint)
                                    pageInfo?.customPaths?.add(customPath) // pageInfo에 customPath 저장
                                    canvas.drawPath(path, drawingPaint)
                                }
                            }
                            invalidate()
                        }
                        ERASER -> { // 지우개 모드
                            eraserPointX = x
                            eraserPointY = y
                            eraserCircleRadius = penInfo.getPenWidth()/2
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    if(selectedPaths.size!=0)
                                        selectedPaths.clear()
                                    setEraser()
                                    viewPager2.isUserInputEnabled = false // 페이지 넘기기 비활성화
                                    PageMode = DRAWING // mode 변경

                                    eraserPath.reset()
                                    eraserPath = Path()
                                    eraserPath.moveTo(x,y)
                                    eraserPoints= CustomPath(PointF(eraserPointX,eraserPointY))
                                    eraserPoints.points.add(PointF(eraserPointX,eraserPointY))
                                    canvas.drawPath(eraserPath,eraserPaint)
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    eraserPath.lineTo(x,y)
                                    eraserPoints.points.add(PointF(eraserPointX,eraserPointY))
                                    canvas.drawPath(eraserPath,eraserPaint)
                                }
                                MotionEvent.ACTION_UP -> {
                                    eraserPoints.points.add(PointF(eraserPointX,eraserPointY))
                                    checkErasePath()
                                    erasedPaths.clear()
                                    eraserPath.reset()
                                    eraserCirclePaint.color = Color.TRANSPARENT
                                }
                            }
                            invalidate()
                        }
                        TEXT->{ //텍스트 모드
//                            textPointX=x
//                            textPointY=y
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {

                                }
                            }
                            invalidate()
                        }
                    }
                }
            }
            true
        }
    }
    fun removeDeletePath(){
        scope= CoroutineScope(Dispatchers.Default).apply {
            launch {
                var isDelete=false
                if (pageInfo?.customPaths != null) {
                    println("customPaths not null")
                    println("pageInfo?.customPaths!!.size : ${pageInfo?.customPaths!!.size}")
                    for (i in pageInfo?.customPaths!!.size-1 downTo 0) {
                        println("i = $i")
                        val line=pageInfo?.customPaths!![i]
                        isDelete=false
                        println("customPaths size = ${line.points.size}")
                        for (p in deletePoints) {
                            for (point in line.points) {
                                if(p.x==point.x&&p.y==point.y){
                                    println("delete point")
                                    pageInfo?.customPaths!!.remove(line)
                                    isDelete=true
                                    break
                                }
                                if(isDelete)
                                    break
                            }
                            if(isDelete)
                                break
                        }
                    }
                }
                deletePoints.clear()
                withContext(Dispatchers.Main){
                    redrawPath(false)
                }
            }
        }
    }

    private fun checkErasePath(){
        scope= CoroutineScope(Dispatchers.Default).apply {
            launch {
                if (pageInfo?.customPaths != null) {
                    for (line in pageInfo?.customPaths!!) {
                        for (point in line.points) {
                            for (p in eraserPoints.points) {
                                if ((penInfo.getPenWidth().toDouble()).pow(2.0) >=
                                    ((p.x - point.x ).toDouble().pow(2.0) +
                                            (p.y - point.y).toDouble().pow(2.0))) {
                                    deletePoints.add(PointF(point.x, point.y))
                                }
                            }
                        }
                    }
                }
                println("find point size = ${deletePoints.size}")
                removeDeletePath()
            }
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
        println("checkContainSelectedPath pageInfo: $pageInfo")

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
        return (point.x in clippingStartPoint!!.x..clippingEndPoint!!.x && point.y in clippingStartPoint!!.y..clippingEndPoint!!.y)
    }

    // mqtt로 선택한 path들 그려서 보내기
    private fun saveCanvas(textType:String){
        val sendBitmap = Bitmap.createScaledBitmap(drawingBitmap, backgroundBitmap.width,
            backgroundBitmap.height, false)
        val selectedCanvas = Canvas(sendBitmap) // 선택된 path 위한 canvas
        selectedCanvas.setBitmap(sendBitmap)
        selectedCanvas.drawColor(Color.WHITE)

        var copiedPaint =  Paint().apply{
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        for(i in 0..selectedPaths.size-1) {
            copiedPaint.strokeWidth = selectedPaths[i].drawingPaint.strokeWidth
            selectedCanvas.drawPath(selectedPaths[i].path, copiedPaint)
        }

        val drawings = IntArray(backgroundBitmap.width * backgroundBitmap.height)
        sendBitmap.getPixels(drawings,0,sendBitmap.width,0,0,sendBitmap.width,sendBitmap.height)

        val returnPixels = ByteArray(drawings.size)

        for(i in drawings.indices){
            val pix = drawings[i]
            var b=pix and 0xff
            if(b!=0)b=255
            returnPixels[i]=(b/255).toByte()
        }
//        pdfActivity.client.sendImageSizeMessage(backgroundBitmap.width)
        when(textType){
//            "English" -> pdfActivity.client.sendEnglishPixelMessage(returnPixels)
//            "Hangul" -> pdfActivity.client.sendHangulPixelMessage(returnPixels)
//            "Number" -> pdfActivity.client.sendNumberPixelMessage(returnPixels)
        }
    }

    fun changePageInfo(pageInfo: PageInfo){
        this.pageInfo = pageInfo
        if(pageInfo.customPaths.size>0){
            for(i in 0..pageInfo.customPaths.size-1){
                var customPath = pageInfo.customPaths[i]
                canvas.drawPath(customPath.path,customPath.drawingPaint)
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
            return spanDelta > SPAN_SLOP && spanDelta < 25
        }
    }
}