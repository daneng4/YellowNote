package com.hansung.yellownote.drawing

import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.hansung.notedatabase.*
import com.hansung.yellownote.R
import com.hansung.yellownote.database.Converters
import com.hansung.yellownote.databinding.ActivityPdfBinding
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*


class PdfActivity() : AppCompatActivity(){
    lateinit var binding : ActivityPdfBinding
    private var pdfReader: PdfReader? = null
    lateinit var viewPager:ViewPager2
    private lateinit var filePath : String
//    lateinit var redBtn:ImageButton
//    lateinit var yellowBtn:ImageButton
//    lateinit var greenBtn:ImageButton
//    lateinit var blueBtn:ImageButton
//    lateinit var blackBtn:ImageButton
    lateinit var penBtn:ImageButton
    lateinit var highlighterBtn:ImageButton
    lateinit var eraserBtn:ImageButton
    lateinit var clippingBtn:ImageButton
    lateinit var textBtn:ImageButton

    private var btnClickTime:Long = 0
    lateinit var color1Btn:Button
    lateinit var color2Btn:Button
    lateinit var color3Btn:Button
    lateinit var recordBtn:ImageButton
    var pageNo = 0
    var firstOpen=true
    val typeConverter=Converters()
    lateinit var myDao : MyDAO // 데이터 베이스
    lateinit var penInfo: PenInfo
    var penWidth = 10F
    var clippingPenWidth = 5F

//    val client=MqttAdapter()

    // DrawingView.kt에서 정의된 mode와 같아야함
    val PenModes = ArrayList<String>(Arrays.asList("PEN","HIGHLIGHTER","ERASER","TEXT","CLIPPING"))
    val PEN = 0
    val HIGHLIGHTER = 1
    val ERASER = 2
    val TEXT = 3
    val CLIPPING = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.out.println("onCreate")
        binding = ActivityPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myDao = MyDatabase.getDatabase(this).getMyDao()
//        getPenDataTable()
        viewPager = binding.viewPager
        viewPager.adapter = PageAdaptor()
        filePath = intent.getStringExtra("filePath")!!
        val noteName=intent.getStringExtra("noteName")?:""

        val targetFile = File(filePath)
        val lastPage=intent.getIntExtra("lastPage",0)
        val afterPageInfo=myDao.getFileDataByFileName(noteName)

        viewPager.currentItem=lastPage
        pdfReader = PdfReader(targetFile, filePath, viewPager).apply {
            println("makePageInfoMap")
//            this.makePageInfoMap(afterPageInfo)
            this.setPageNumberToPageInfo(lastPage)
            (viewPager.adapter as PageAdaptor).setupPdfRenderer(this)
            pageNo = lastPage
            viewPager.setCurrentItem(lastPage,false)
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(page: Int) {
                super.onPageSelected(page)

                pageNo = page
                println("viewPager callback 함수 ")
                // position에 해당하는 pageInfo 가져오기
                if(!pdfReader!!.pageInfoMap.containsKey(page)) { // page에 해당하는 pageInfo가 없는 경우
                    println("새로운 pageInfo 생성")
                    pdfReader!!.pageInfoMap[page] = PageInfo(page) // 새로운 pageInfo 생성
                }
                println("page : $page")
                println("pageInfo : ${pdfReader!!.pageInfoMap[page]?.customPaths}")
                pdfReader!!.pageInfoMap[page]?.let {
                    pdfReader!!.changePageInfo(it) } // 변경된 page의 pageInfo 세팅
//                System.out.println("Page$position path개수 = ${pdfReader!!.pageInfoMap[position]?.customPaths?.size}")
            }
        })

//        System.out.println("${this.viewPager.rootView}")
        penInfo = ViewModelProvider(this)[PenInfo::class.java]
        penInfo.setPenColor(intent.getIntExtra("penColor",Color.BLACK))
        penInfo.setPenWidth(intent.getFloatExtra("penWidth",10F))
        penInfo.setPenMode(intent.getIntExtra("penMode",PEN))


//        redBtn = binding.RedPenBtn
//        yellowBtn = binding.YellowPenBtn
//        greenBtn = binding.GreenPenBtn
//        blueBtn = binding.BluePenBtn
//        blackBtn = binding.BlackPenBtn
        penBtn = binding.PenBtn
        highlighterBtn = binding.HighlighterBtn

        clippingBtn = binding.ClippingBtn
        eraserBtn = binding.EraserBtn
        textBtn = binding.TextBtn
        color1Btn = binding.color1Btn
        color2Btn = binding.color2Btn
        color3Btn = binding.color3Btn

        color1Btn.setOnClickListener {
            setColorBtn(color1Btn)
        }
        color2Btn.setOnClickListener {
            setColorBtn(color2Btn)
        }
        color3Btn.setOnClickListener {
            setColorBtn(color3Btn)
        }


//        redBtn.setOnClickListener {
//            System.out.println("Click redBtn")
//            setPenData(Color.RED,penWidth, PEN)
//        }
//        yellowBtn.setOnClickListener {
//            System.out.println("Click yellowBtn")
//            setPenData(Color.YELLOW,penWidth, PEN)
//        }
//
//        greenBtn.setOnClickListener {
//            System.out.println("Click greenBtn")
//            setPenData(Color.GREEN,penWidth, PEN)
//        }
//        blueBtn.setOnClickListener {
//            System.out.println("Click blueBtn")
//            setPenData(Color.BLUE,penWidth, PEN)
//        }
//        blackBtn.setOnClickListener {
//            System.out.println("Click blackBtn")
//            setPenData(Color.BLACK,penWidth, PEN)
//        }
        penBtn.setOnClickListener{
            changeBtnImage(PEN)
        }
        highlighterBtn.setOnClickListener {
            changeBtnImage(HIGHLIGHTER)
        }
        eraserBtn.setOnClickListener {
            changeBtnImage(ERASER)
        }
        clippingBtn.setOnClickListener {
            changeBtnImage(CLIPPING)
        }
        textBtn.setOnClickListener {
            changeBtnImage(TEXT)
//            pdfReader!!.setMode("text")
        }
        println("onCreate끝")
    }

    private fun setColorBtn(button:Button){
        if (System.currentTimeMillis() > btnClickTime + 1000) {
            btnClickTime = System.currentTimeMillis()
//            button.backgroundTintList.getColorForState()
        }
        else{
            ColorPickerDialog.Builder(this,android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).apply{
                setTitle("")
                setPreferenceName("ColorPickerDialog")
                setPositiveButton("SELECT",ColorEnvelopeListener(){ colorEnvelope: ColorEnvelope, b: Boolean ->
                    button.setBackgroundTintList(ColorStateList.valueOf(colorEnvelope.color))
                })
                setNegativeButton("CANCEL", DialogInterface.OnClickListener(){
                        dialog: DialogInterface?, which: Int ->  dialog!!.dismiss()
                })
                attachAlphaSlideBar(false)
                attachBrightnessSlideBar(true)
                setBottomSpace(12)
            }.show()
        }
    }

    // 펜, 형광펜, 지우개, 그물, 텍스트 버튼 이미지 변경
    private fun changeBtnImage(mode:Int){
        if(penInfo.getPenMode()!=mode){
            when(penInfo.getPenMode()){
                PEN -> penBtn.setImageResource(R.drawable.ic_pen)
                HIGHLIGHTER -> highlighterBtn.setImageResource(R.drawable.ic_highlighter)
                ERASER -> eraserBtn.setImageResource(R.drawable.ic_eraser)
                CLIPPING -> clippingBtn.setImageResource(R.drawable.ic_lasso)
                TEXT -> textBtn.setImageResource(R.drawable.ic_text)
            }
        }

        when(mode){
            PEN->{
                penBtn.setImageResource(R.drawable.ic_pen_clicked)
                setPenData(Color.BLACK, penWidth, PEN)
            }
            HIGHLIGHTER->{
                highlighterBtn.setImageResource(R.drawable.ic_highlighter_clicked)
                setPenData(Color.BLACK, penWidth, HIGHLIGHTER)
            }
            ERASER->{
                eraserBtn.setImageResource(R.drawable.ic_eraser_clicked)
                setPenData(null, penWidth, ERASER)
            }
            CLIPPING->{
                clippingBtn.setImageResource(R.drawable.ic_lasso_clicked)
                setPenData(Color.GRAY,clippingPenWidth, CLIPPING)
            }
            TEXT->{
                System.out.println("click text")
                textBtn.setImageResource(R.drawable.ic_text_clicked)
                setPenData(Color.BLACK,10F, TEXT)
            }
        }
    }

    private fun setPenData(color:Int?, width:Float, penMode:Int){
        System.out.println("${PenModes[penInfo.getPenMode()]} -> ${PenModes[penMode]}")
        if(penInfo.getPenMode()!=penMode){
            myDao.updatePenData(PenModes[penInfo.getPenMode()],penInfo.getPenWidth(),penInfo.getPenColor(),false)
        }
        myDao.updatePenData(PenModes[penMode],width,color,true)
        penInfo.setPenMode(penMode)
        if (penMode != ERASER) {
            penInfo.setPenColor(color!!)
        }
        else
            penInfo.setPenColor(null)
        penInfo.setPenWidth(width)
    }

    private fun getPenDataTable(){
        CoroutineScope(Dispatchers.Main).launch {
            myDao.getAllPenData()
        }
    }

    private fun updatePenDatatable(){
        CoroutineScope(Dispatchers.Main).launch {
//            myDao.updatePenData()
        }
    }

    override fun onPause() {
        super.onPause()
        val pathArray=filePath.split('/').last()
        val noteName=pathArray.split('.')[0]
        // DB에 스키마 insert

        runBlocking {
            myDao.insertNoteData(NoteData(noteName, pageNo, filePath))
        }
        val pageCount=pdfReader!!.pageCount
        for(i in 0..pageCount){
            val drawingInfo=pdfReader!!.pageInfoMap[i]

            if(drawingInfo!=null) {
                if(drawingInfo.customPaths.isNotEmpty()) {
//                    val stream = MemoryStream(arrByte)
                    runBlocking {
                        myDao.insertFileData(FileData(noteName ,drawingInfo))
                    }

                }
            }
        }

    }
    override fun onDestroy() {
        super.onDestroy()

        pdfReader?.close()
    }
}