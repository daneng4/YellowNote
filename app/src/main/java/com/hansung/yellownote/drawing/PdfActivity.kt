package com.hansung.yellownote.drawing

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.annotation.RequiresApi
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
import kotlinx.android.synthetic.main.activity_pdf.*
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
    lateinit var penBtn:ImageButton
    lateinit var highlighterBtn:ImageButton
    lateinit var eraserBtn:ImageButton
    lateinit var clippingBtn:ImageButton
    lateinit var textBtn:ImageButton

    private var btnClickTime:Long = 0
    lateinit var ColorButton1:Button
    lateinit var ColorButton2:Button
    lateinit var ColorButton3:Button

    var color1:Int = Color.BLACK
    var color2:Int = Color.BLACK
    var color3:Int = Color.BLACK
    var color4:Int = Color.BLACK
    var color5:Int = Color.BLACK
    var color6:Int = Color.BLACK

    lateinit var recordBtn:ImageButton
    var pageNo = 0
    var firstOpen=true
    val typeConverter=Converters()
    lateinit var myDao : MyDAO // 데이터 베이스
    lateinit var penInfo: PenInfo
    var penWidth = 10F
    var clippingPenWidth = 5F

    private var penSettingPopup:PenSettingDialog? = null
    private var highlighterSettingPopup:HighlighterSettingDialog? = null
//    val client=MqttAdapter()

    // DrawingView.kt에서 정의된 mode와 같아야함
    val PenModes = ArrayList<String>(Arrays.asList("PEN","HIGHLIGHTER","ERASER","TEXT","CLIPPING"))
    val PEN = 0
    val HIGHLIGHTER = 1
    val ERASER = 2
    val TEXT = 3
    val CLIPPING = 4

    @RequiresApi(Build.VERSION_CODES.O)
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
//            println("makePageInfoMap")
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

        penBtn = binding.PenBtn
        highlighterBtn = binding.HighlighterBtn

        clippingBtn = binding.ClippingBtn
        eraserBtn = binding.EraserBtn
        textBtn = binding.TextBtn
        ColorButton1 = binding.ColorButton1
        ColorButton2 = binding.ColorButton2
        ColorButton3 = binding.ColorButton3

        color1 = intent.getIntExtra("ColorButton1", Color.BLACK)
        color2 = intent.getIntExtra("ColorButton2", Color.BLACK)
        color3 = intent.getIntExtra("ColorButton3", Color.BLACK)
        color4 = intent.getIntExtra("ColorButton4", Color.BLACK)
        color5 = intent.getIntExtra("ColorButton5", Color.BLACK)
        color6 = intent.getIntExtra("ColorButton6", Color.BLACK)

        System.out.println("color1 = ${color1}")
        System.out.println("color2 = ${color2}")
        System.out.println("color3 = ${color3}")
        System.out.println("color4 = ${color4}")
        System.out.println("color5 = ${color5}")
        System.out.println("color6 = ${color6}")

        settingColorButton()

        ColorButton1.setOnClickListener {
            setColorBtn(ColorButton1)
        }
        ColorButton2.setOnClickListener {
            setColorBtn(ColorButton2)
        }
        ColorButton3.setOnClickListener {
            setColorBtn(ColorButton3)
        }

        penBtn.setOnClickListener{
            System.out.println("click")
            if (penBtn.tag == R.drawable.ic_pen_clicked) {
                System.out.println("penBtn.tag == R.drawable.ic_pen_clicked")

                if(penSettingPopup == null){
                    System.out.println("penSettingPopup == null")
                    var location = IntArray(2)
                    penBtn.getLocationOnScreen(location)
                    penSettingPopup = PenSettingDialog(this)
                    penSettingPopup!!.show(penInfo,location[0],location[1])
                }
                else{
                    if(penSettingPopup!!.isDialogShowing()){
                        System.out.println("${penSettingPopup!!.isDialogShowing()}")
                        System.out.println("penSettingPopup != null")
                        penSettingPopup!!.dismiss()
                        penSettingPopup = null
                    }
                    else{
                        var location = IntArray(2)
                        penBtn.getLocationOnScreen(location)
                        penSettingPopup = PenSettingDialog(this)
                        penSettingPopup!!.show(penInfo,location[0],location[1])
                    }
                }
            } else {
                System.out.println("penBtn.tag != R.drawable.ic_pen_clicked")
                changeBtnImage(PEN)
            }
        }
        highlighterBtn.setOnClickListener {
            if (highlighterBtn.tag == R.drawable.ic_highlighter_clicked) {
                if(highlighterSettingPopup == null){
                    var location = IntArray(2)
                    highlighterBtn.getLocationOnScreen(location)
                    highlighterSettingPopup = HighlighterSettingDialog(this)
                    highlighterSettingPopup!!.show(penInfo, 30, location[0],location[1])
                }
                else{
                    highlighterSettingPopup!!.dismiss()
                    highlighterSettingPopup = null
                }
            } else {
                changeBtnImage(HIGHLIGHTER)
            }
        }
        eraserBtn.setOnClickListener {
            changeBtnImage(ERASER)
        }
        clippingBtn.setOnClickListener {
            changeBtnImage(CLIPPING)
        }
        textBtn.setOnClickListener {
            changeBtnImage(TEXT)
        }

        penInfo = ViewModelProvider(this)[PenInfo::class.java]
        penInfo.setPenColor(intent.getIntExtra("penColor",Color.BLACK))
        penInfo.setPenWidth(intent.getFloatExtra("penWidth",10F))
        penInfo.setPenMode(intent.getIntExtra("penMode",PEN))
        changeBtnImage(penInfo.getPenMode())


        println("onCreate끝")


    }

    // 펜 색상 선택 버튼 색깔 변경
    private fun settingColorButton(){
        ColorButton1.setBackgroundTintList(ColorStateList.valueOf(color1))
        ColorButton2.setBackgroundTintList(ColorStateList.valueOf(color2))
        ColorButton3.setBackgroundTintList(ColorStateList.valueOf(color3))
        ColorButton4.setBackgroundTintList(ColorStateList.valueOf(color4))
        ColorButton5.setBackgroundTintList(ColorStateList.valueOf(color5))
        ColorButton6.setBackgroundTintList(ColorStateList.valueOf(color6))
    }

    private fun setColorBtn(button:Button){
        if (System.currentTimeMillis() > btnClickTime + 1000) {
            btnClickTime = System.currentTimeMillis()
            penInfo.setPenColor(button.backgroundTintList.hashCode())
            System.out.println("button.backgroundTintList.hashCode() = ${button.backgroundTintList.hashCode()}")
//            button.backgroundTintList.getColorForState()
        }
        else{
            ColorPickerDialog.Builder(this,android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).apply{
                setTitle("")
                setPreferenceName("ColorPickerDialog")
                setPositiveButton("SELECT",ColorEnvelopeListener(){ colorEnvelope: ColorEnvelope, b: Boolean ->
                    button.setBackgroundTintList(ColorStateList.valueOf(colorEnvelope.color))
                    penInfo.setPenColor(colorEnvelope.color)
                    myDao.updateColorData(resources.getResourceEntryName(button.id), colorEnvelope.color)
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
                PEN -> {
                    penBtn.tag = R.drawable.ic_pen
                    penBtn.setImageResource(R.drawable.ic_pen)
                }
                HIGHLIGHTER -> highlighterBtn.setImageResource(R.drawable.ic_highlighter)
                ERASER -> eraserBtn.setImageResource(R.drawable.ic_eraser)
                CLIPPING -> clippingBtn.setImageResource(R.drawable.ic_lasso)
                TEXT -> textBtn.setImageResource(R.drawable.ic_text)
            }
        }

        when(mode){
            PEN->{
                penBtn.tag = R.drawable.ic_pen_clicked
                penBtn.setImageResource(R.drawable.ic_pen_clicked)
                setPenData(Color.BLACK, penWidth, PEN)
            }
            HIGHLIGHTER->{
                highlighterBtn.tag = R.drawable.ic_highlighter_clicked
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
        System.out.println("PenMode = ${penMode}, penInfo = ${penInfo}, penInfo.getPenMode() = ${penInfo.getPenMode()}")
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
                    runBlocking {
                        myDao.insertFileData(FileData(noteName ,drawingInfo))
                    }

                }
            }
        }

    }
    override fun onDestroy() {
        super.onDestroy()

        //다이얼로그가 띄워져 있는 상태(showing)인 경우 dismiss() 호출
        if (penSettingPopup != null) {
            penSettingPopup!!.dismiss()
            penSettingPopup = null
        }
        else if (highlighterSettingPopup != null) {
            highlighterSettingPopup!!.dismiss()
            highlighterSettingPopup = null
        }

        pdfReader?.close()
    }

}