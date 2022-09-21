package com.hansung.yellownote.drawing

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.hansung.notedatabase.*
import com.hansung.yellownote.AudioAdapter
import com.hansung.yellownote.MainActivity
import com.hansung.yellownote.R
import com.hansung.yellownote.SocketAdapter
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
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.*


class PdfActivity() : AppCompatActivity(){

    private var audioRecordImageBtn: ImageButton? = null

    /**오디오 파일 관련 변수 */ // 오디오 권한
    private val recordPermission: String = Manifest.permission.RECORD_AUDIO
    private val PERMISSION_CODE = 21

    // 오디오 파일 녹음 관련 변수
    private var mediaRecorder: MediaRecorder? = null
    private var audioFileName // 오디오 녹음 생성 파일 이름
            : String? = null
    private var isRecording = false // 현재 녹음 상태를 확인하기 위함.
    private var audioUri: Uri? = null // 오디오 파일 uri

    // 오디오 파일 재생 관련 변수
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    var playIcon: ImageView? = null

    /** 리사이클러뷰  */
    private var audioAdapter: AudioAdapter? = null
    private var audioList: ArrayList<Uri>? = null

    //***녹음 전역변수들***
    var client: SocketAdapter = SocketAdapter()
    var sendRawData:String? = null
    //private var connectbtn: Button? = null
    var globalfilepath: String? = null

    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var BufferElements2Rec = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024
    private var BytesPerElement = 2 // 2 bytes in 16bit format

    lateinit var binding : ActivityPdfBinding
    private var pdfReader: PdfReader? = null
    lateinit var viewPager:ViewPager2
    private lateinit var filePath : String
    lateinit var penBtn:ImageButton
    lateinit var eraserBtn:ImageButton
    lateinit var clippingBtn:ImageButton
    lateinit var textBtn:ImageButton

    lateinit var drawingView:DrawingView

    private var btnClickTime:Long = 0
    lateinit var ColorButton1:Button
    lateinit var ColorButton2:Button
    lateinit var ColorButton3:Button
    lateinit var ColorButton4:Button
    lateinit var ColorButton5:Button
    lateinit var ColorButton6:Button

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
    private var eraserSettingPopup:PenSettingDialog? = null
//    val client=MqttAdapter()

    // DrawingView.kt에서 정의된 mode와 같아야함
    val PenModes = ArrayList<String>(Arrays.asList("PEN","ERASER","TEXT","CLIPPING"))
    val PEN = 0
    val ERASER = 1
    val TEXT = 2
    val CLIPPING = 3

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        binding = ActivityPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
//        client = MqttAdapter()

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
            this.makePageInfoMap(afterPageInfo)
            if(pageInfoMap[lastPage] != null){
                pageInfo = pageInfoMap[lastPage]!!
            }
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
        clippingBtn = binding.ClippingBtn
        eraserBtn = binding.EraserBtn
        textBtn = binding.TextBtn
        ColorButton1 = binding.ColorButton1
        ColorButton2 = binding.ColorButton2
        ColorButton3 = binding.ColorButton3
        ColorButton4 = binding.ColorButton4
        ColorButton5 = binding.ColorButton5
        ColorButton6 = binding.ColorButton6

        color1 = intent.getIntExtra("ColorButton1", Color.BLACK)
        color2 = intent.getIntExtra("ColorButton2", Color.BLACK)
        color3 = intent.getIntExtra("ColorButton3", Color.BLACK)
        color4 = intent.getIntExtra("ColorButton4", Color.BLACK)
        color5 = intent.getIntExtra("ColorButton5", Color.BLACK)
        color6 = intent.getIntExtra("ColorButton6", Color.BLACK)

        settingColorButton()

        setButtonHandlers()

        ColorButton1.setOnClickListener {
            setColorBtn(ColorButton1)
        }
        ColorButton2.setOnClickListener {
            setColorBtn(ColorButton2)
        }
        ColorButton3.setOnClickListener {
            setColorBtn(ColorButton3)
        }
        ColorButton4.setOnClickListener {
            setColorBtn(ColorButton4)
        }
        ColorButton5.setOnClickListener {
            setColorBtn(ColorButton5)
        }
        ColorButton6.setOnClickListener {
            setColorBtn(ColorButton6)
        }

        penBtn.setOnClickListener{
            System.out.println("click")
            if (penBtn.tag == R.drawable.ic_pen_clicked) {
                // clipping 네모 표시 있으면 없애기
                checkResetClipping()

                if (penBtn.tag == R.drawable.ic_pen_clicked) {
                    if (penSettingPopup == null) {
                        System.out.println("penSettingPopup == null")
                        penSettingPopup = PenSettingDialog(this)
                        penSettingPopup!!.show(myDao, penInfo)
                    }
                }
                else{
                    if(penSettingPopup!!.isDialogShowing()){
                        System.out.println("${penSettingPopup!!.isDialogShowing()}")
                        penSettingPopup!!.dismiss()
                        penSettingPopup = null
                    }
                    else{
                        penSettingPopup = PenSettingDialog(this)
                        penSettingPopup!!.show(myDao, penInfo)
                    }
                }
            } else {
                System.out.println("penBtn.tag != R.drawable.ic_pen_clicked")
                changeBtnImage(PEN)
            }
        }

        eraserBtn.setOnClickListener{
            // clipping 네모 표시 있으면 없애기
            checkResetClipping()

            if (eraserBtn.tag == R.drawable.ic_eraser_clicked) {
                if(eraserSettingPopup == null){
                    var location = IntArray(2)
                    eraserBtn.getLocationOnScreen(location)
                    eraserSettingPopup = PenSettingDialog(this)
                    eraserSettingPopup!!.show(myDao, penInfo)
                    eraserSettingPopup!!.changeText()
                }
                else{
                    if(eraserSettingPopup!!.isDialogShowing()){
                        System.out.println("${eraserSettingPopup!!.isDialogShowing()}")
                        eraserSettingPopup!!.dismiss()
                        eraserSettingPopup = null
                    }
                    else{
                        var location = IntArray(2)
                        penBtn.getLocationOnScreen(location)
                        eraserSettingPopup = PenSettingDialog(this)
                        eraserSettingPopup!!.show(myDao, penInfo)
                        eraserSettingPopup!!.changeText()
                    }
                }
            } else {
                changeBtnImage(ERASER)
            }
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

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun init() {
        audioRecordImageBtn = findViewById(R.id.btnStart)
        audioRecordImageBtn!!.setOnClickListener {
            if (isRecording) {
                // 현재 녹음 중 O
                // 녹음 상태에 따른 변수 아이콘 & 텍스트 변경
                isRecording = false // 녹음 상태 값
                audioRecordImageBtn!!.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.record,
                        null
                    )
                ) // 녹음 상태 아이콘 변경
                stopRecording()
                // 녹화 이미지 버튼 변경 및 리코딩 상태 변수값 변경
            } else {
                // 현재 녹음 중 X
                /*절차
                         *       1. Audio 권한 체크
                         *       2. 처음으로 녹음 실행한건지 여부 확인
                         * */
                if (checkAudioPermission()) {
                    // 녹음 상태에 따른 변수 아이콘 & 텍스트 변경
                    isRecording = true // 녹음 상태 값
                    audioRecordImageBtn!!.setImageDrawable(
                        resources.getDrawable(
                            R.drawable.stopped,
                            null
                        )
                    ) // 녹음 상태 아이콘 변경
                    startRecording()
                }
            }
        }

        // 리사이클러뷰
        val audioRecyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        audioList = ArrayList()
        audioAdapter = AudioAdapter(this, audioList)
        audioRecyclerView.adapter = audioAdapter
        val mLayoutManager = LinearLayoutManager(this)
        mLayoutManager.orientation = LinearLayoutManager.VERTICAL
        audioRecyclerView.layoutManager = mLayoutManager

        // 커스텀 이벤트 리스너 4. 액티비티에서 커스텀 리스너 객체 생성 및 전달
        audioAdapter!!.setOnItemClickListener(object : AudioAdapter.OnIconClickListener {
            override fun onItemClick(view: View?, position: Int) {
                val uriName: String = java.lang.String.valueOf(audioList!![position])

                /*음성 녹화 파일에 대한 접근 변수 생성;
                     (ImageView)를 붙여줘서 View 객체를 형변환 시켜줌.
                     전역변수로 한 이유는
                    * */
                val file = File(uriName)
                if (isPlaying) {
                    // 음성 녹화 파일이 여러개를 클릭했을 때 재생중인 파일의 Icon을 비활성화(비 재생중)으로 바꾸기 위함.
                    if (playIcon === view as ImageView) {
                        // 같은 파일을 클릭했을 경우
                        startRecording()
                    } else {
                        // 다른 음성 파일을 클릭했을 경우
                        // 기존의 재생중인 파일 중지
                        stopRecording()

                        // 새로 파일 재생하기
                        playIcon = view as ImageView
                        playAudio(file)
                    }
                } else {
                    playIcon = view as ImageView
                    playAudio(file)
                }
            }
        })
    }

    // 녹음 파일 재생
    private fun playAudio(file: File) {
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer!!.setDataSource(file.absolutePath)
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        playIcon?.setImageDrawable(resources.getDrawable(R.drawable.ic_audio_pause, null))
        isPlaying = true
        mediaPlayer!!.setOnCompletionListener { stopAudio() }
    }

    // 녹음 파일 중지
    private fun stopAudio() {
        playIcon?.setImageDrawable(resources.getDrawable(R.drawable.ic_play, null))
        isPlaying = false
        mediaPlayer!!.stop()
    }

    // 오디오 파일 권한 체크
    private fun checkAudioPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                applicationContext,
                recordPermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(recordPermission), PERMISSION_CODE)
            false
        }
    }

    private fun setButtonHandlers() {
        (findViewById<View>(R.id.btnStart) as ImageButton).setOnClickListener(btnClick)
        (findViewById<View>(R.id.btnStop) as ImageButton).setOnClickListener(btnClick)
        (findViewById<View>(R.id.connectBtn) as ImageButton).setOnClickListener(btnClick)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private val btnClick: View.OnClickListener = View.OnClickListener { v ->
        when (v.id) {
            R.id.btnStart -> {
                System.out.println("btnStart 누름")
                btnStart.setImageDrawable(resources.getDrawable(R.drawable.record, null))
                startRecording()

            }
            R.id.btnStop -> {
                System.out.println("btnStop 누름")
                btnStart.setImageDrawable(resources.getDrawable(R.drawable.beforecord, null))
                stopRecording()
            }
            R.id.connectBtn -> {
                System.out.println("connect버튼 누름")
                sendRawData = globalfilepath
                if(sendRawData!=null) {
                    System.out.println("sendRawData 보내기")
                    //client.sendAudioFileMessage(sendRawData!!)
                    client.connect(sendRawData!!)
                }
            }
        }
    }

    private fun stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false
            recorder!!.stop()
            recorder!!.release()
            //noiseSuppressor?.release()
            recorder = null
            recordingThread = null
            audioUri = Uri.parse(globalfilepath)
            audioList?.add(audioUri!!)
            audioAdapter!!.notifyDataSetChanged()
        }
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            finish()
            return
        }
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            MainActivity.RECORDER_SAMPLERATE, MainActivity.RECORDER_CHANNELS,
            MainActivity.RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement
        )

        //enableNoiseSuppressor()
        recorder!!.startRecording()
        isRecording = true
        recordingThread = Thread({ writeAudioDataToFile() }, "AudioRecorder Thread")
        recordingThread!!.start()


    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SimpleDateFormat")
    private fun writeAudioDataToFile() {
        val timeStamp: String = SimpleDateFormat("yyMMdd_HHmm").format(Date())
        // Write the output audio in byte
        val filePath =
            filesDir.absolutePath + '/' + "recTest_" + timeStamp + "_audio.pcm"
        val filePath2 = filesDir.absolutePath + '/' + "Record_" + timeStamp + "_audio.wav"
        globalfilepath = filePath2
        val sData = ShortArray(BufferElements2Rec)
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(filePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        while (isRecording) {

            // gets the voice output from microphone to byte format
            recorder!!.read(sData, 0, BufferElements2Rec)
            DriverManager.println("Short writing to file$sData")
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                val bData = short2byte(sData)
                os?.write(bData, 0, BufferElements2Rec * BytesPerElement)
                val f1 = File(filePath)
                val f2 = File(filePath2)
                rawToWave(f1,f2)
                //sendRawData = bData
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        try {
            os?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(IOException::class)
    fun rawToWave(rawFile: File, waveFile: File) {
        val rawData = ByteArray(rawFile.length().toInt())
        //sendRawData = rawData

        var input: DataInputStream? = null
        try {
            input = DataInputStream(FileInputStream(rawFile))
            input.read(rawData)
        } finally {
            input?.close()
        }
        var output: DataOutputStream? = null
        try {
            output = DataOutputStream(FileOutputStream(waveFile))
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF") // chunk id
            writeInt(output, 36 + rawData.size) // chunk size
            writeString(output, "WAVE") // format
            writeString(output, "fmt ") // subchunk 1 id
            writeInt(output, 16) // subchunk 1 size
            writeShort(output, 1.toShort()) // audio format (1 = PCM)
            writeShort(output, 1.toShort()) // number of channels
            writeInt(output, 44100) // sample rate
            writeInt(output, MainActivity.RECORDER_SAMPLERATE * 2) // byte rate
            writeShort(output, 2.toShort()) // block align
            writeShort(output, 16.toShort()) // bits per sample
            writeString(output, "data") // subchunk 2 id
            writeInt(output, rawData.size) // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            val shorts = ShortArray(rawData.size / 2)
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            val bytes: ByteBuffer = ByteBuffer.allocate(shorts.size * 2)
            for (s in shorts) {
                bytes.putShort(s)
            }
            output.write(fullyReadFileToBytes(rawFile))

        } finally {
            output?.close()
        }
    }

    @Throws(IOException::class)
    fun fullyReadFileToBytes(f: File): ByteArray {
        val size = f.length().toInt()
        val bytes = ByteArray(size)
        val tmpBuff = ByteArray(size)
        val fis = FileInputStream(f)
        try {
            var read: Int = fis.read(bytes, 0, size)
            if (read < size) {
                var remain = size - read
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain)
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read)
                    remain -= read
                }
            }
        } catch (e: IOException) {
            throw e
        } finally {
            fis.close()
        }
        return bytes
    }

    @Throws(IOException::class)
    private fun writeInt(output: DataOutputStream?, value: Int) {
        output?.write(value shr 0)
        output?.write(value shr 8)
        output?.write(value shr 16)
        output?.write(value shr 24)
    }

    @Throws(IOException::class)
    private fun writeShort(output: DataOutputStream?, value: Short) {
        output?.writeByte(value.toInt() shr 0) // write byte 로 노이즈해결
        output?.writeByte(value.toInt() shr 8)
    }

    @Throws(IOException::class)
    private fun writeString(output: DataOutputStream?, value: String) {
        for (element in value) {
            output?.writeByte(element.code)
        }
    }

    private fun short2byte(sData: ShortArray): ByteArray {
        val shortArrsize = sData.size
        val bytes = ByteArray(shortArrsize * 2)
        for (i in 0 until shortArrsize) {
            bytes[i * 2] = (sData[i].toInt() and 0x00FF).toByte()
            bytes[i * 2 + 1] = (sData[i].toInt() shr 8).toByte()
            sData[i] = 0
        }
        return bytes
    }

    private fun checkResetClipping(){
        if(drawingView.selectedPaths.size>0){
            drawingView.selectedPaths.clear()
            if(drawingView.popupWindow!=null)
                drawingView.popupWindow.dismiss()
            drawingView.clippingStartPoint = PointF(-1f,-1f)
            drawingView.clippingEndPoint = PointF(-1f,-1f)
            drawingView.invalidate()
        }
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
            when(button){
                ColorButton1 -> penInfo.setPenColor(color1)
                ColorButton2 -> penInfo.setPenColor(color2)
                ColorButton3 -> penInfo.setPenColor(color3)
                ColorButton4 -> penInfo.setPenColor(color4)
                ColorButton5 -> penInfo.setPenColor(color5)
                ColorButton6 -> penInfo.setPenColor(color6)
            }
            myDao.updatePenData(PenModes[penInfo.getPenMode()],penInfo.getPenWidth(),penInfo.getPenColor(),true)
        }
        else{
            ColorPickerDialog.Builder(this,android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).apply{
                setTitle("")
                setPreferenceName("ColorPickerDialog")
                setPositiveButton("SELECT",ColorEnvelopeListener(){ colorEnvelope: ColorEnvelope, b: Boolean ->
                    button.setBackgroundTintList(ColorStateList.valueOf(colorEnvelope.color))
                    penInfo.setPenColor(colorEnvelope.color)
                    when(button){
                        ColorButton1 -> color1 = colorEnvelope.color
                        ColorButton2 -> color2 = colorEnvelope.color
                        ColorButton3 -> color3 = colorEnvelope.color
                        ColorButton4 -> color4 = colorEnvelope.color
                        ColorButton5 -> color5 = colorEnvelope.color
                        ColorButton6 -> color6 = colorEnvelope.color
                    }
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
                ERASER -> {
                    eraserBtn.tag = R.drawable.ic_eraser
                    eraserBtn.setImageResource(R.drawable.ic_eraser)
                }
                CLIPPING -> clippingBtn.setImageResource(R.drawable.ic_lasso)
                TEXT -> textBtn.setImageResource(R.drawable.ic_text)
            }
        }

        when(mode){
            PEN->{
                penBtn.tag = R.drawable.ic_pen_clicked
                penBtn.setImageResource(R.drawable.ic_pen_clicked)
                setPenData(myDao.getAllPenData()[PEN].color, myDao.getAllPenData()[PEN].width, PEN)
            }
            ERASER->{
                eraserBtn.tag = R.drawable.ic_eraser_clicked
                eraserBtn.setImageResource(R.drawable.ic_eraser_clicked)
                setPenData(null, myDao.getAllPenData()[ERASER].width, ERASER)
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

        pdfReader?.close()
    }

}