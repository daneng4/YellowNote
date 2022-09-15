package com.hansung.yellownote.drawing

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.hansung.notedatabase.FileData
import com.hansung.notedatabase.MyDAO
import com.hansung.notedatabase.MyDatabase
import com.hansung.notedatabase.NoteData
import com.hansung.yellownote.database.Converters
import com.hansung.yellownote.databinding.ActivityPdfBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

class PdfActivity() : AppCompatActivity(){
//class PdfActivity() : AppCompatActivity(){
    lateinit var binding : ActivityPdfBinding
    private var pdfReader: PdfReader? = null
    lateinit var viewPager:ViewPager2
    private lateinit var filePath : String
    lateinit var redBtn:ImageButton
    lateinit var yellowBtn:ImageButton
    lateinit var greenBtn:ImageButton
    lateinit var blueBtn:ImageButton
    lateinit var blackBtn:ImageButton
    lateinit var eraserBtn:ImageButton
    lateinit var clippingBtn:ImageButton
    lateinit var textBtn:ImageButton
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
    val PenModes = ArrayList<String>(Arrays.asList("PEN","ERASER","TEXT","CLIPPING","SHAPE"))
    val PEN = 0
    val ERASER = 1
    val TEXT = 2
    val CLIPPING = 3
    val SHAPE = 4

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
            this.makePageInfoMap(afterPageInfo)
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
                pdfReader!!.pageInfoMap[page]?.let { println("setDrawingViewPageInfo")
                    pdfReader!!.setDrawingViewPageInfo(it) } // 변경된 page의 pageInfo 세팅
//                System.out.println("Page$position path개수 = ${pdfReader!!.pageInfoMap[position]?.customPaths?.size}")
            }
        })

//        System.out.println("${this.viewPager.rootView}")
        penInfo = ViewModelProvider(this)[PenInfo::class.java]
        penInfo.setPenColor(intent.getIntExtra("penColor",Color.BLACK))
        penInfo.setPenWidth(intent.getFloatExtra("penWidth",10F))
        penInfo.setPenMode(intent.getIntExtra("penMode",PEN))


        redBtn = binding.RedPenBtn
        yellowBtn = binding.YellowPenBtn
        greenBtn = binding.GreenPenBtn
        blueBtn = binding.BluePenBtn
        blackBtn = binding.BlackPenBtn
        clippingBtn = binding.ClippingBtn
        eraserBtn = binding.EraserBtn
        textBtn = binding.TextBtn


        redBtn.setOnClickListener {
            System.out.println("Click redBtn")
            setPenData(Color.RED,penWidth, PEN)
        }
        yellowBtn.setOnClickListener {
            System.out.println("Click yellowBtn")
            setPenData(Color.YELLOW,penWidth, PEN)
        }

        greenBtn.setOnClickListener {
            System.out.println("Click greenBtn")
            setPenData(Color.GREEN,penWidth, PEN)
        }
        blueBtn.setOnClickListener {
            System.out.println("Click blueBtn")
            setPenData(Color.BLUE,penWidth, PEN)
        }
        blackBtn.setOnClickListener {
            System.out.println("Click blackBtn")
            setPenData(Color.BLACK,penWidth, PEN)
        }
        eraserBtn.setOnClickListener {
            setPenData(null, penWidth, ERASER)
        }
        clippingBtn.setOnClickListener {
            System.out.println("CLIPPING CLICK")
            setPenData(Color.GRAY,clippingPenWidth, CLIPPING)
        }
        textBtn.setOnClickListener {
//            pdfReader!!.setMode("text")
        }
        println("onCreate끝")
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
                        myDao.insertFileData(FileData(noteName, drawingInfo))
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