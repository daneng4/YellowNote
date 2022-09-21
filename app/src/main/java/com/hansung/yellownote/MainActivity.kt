package com.hansung.yellownote

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Point
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils

import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.hansung.notedatabase.*
import com.hansung.yellownote.databinding.ActivityMainBinding
import com.hansung.yellownote.drawing.PenInfo
import com.hansung.yellownote.drawing.PdfActivity
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
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var permissions = arrayOf(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO)
    var isKitKat = false
    private lateinit var linearLayout:LinearLayout
    lateinit var myDao : MyDAO // 데이터 베이스
    lateinit var penInfo: PenInfo // 펜 정보 담고 있는 뷰모델
    val PenModes = ArrayList<String>(Arrays.asList("PEN","ERASER","TEXT","CLIPPING","SHAPE"))
    lateinit var noteList:List<NoteData>
    val deleteButtons=ArrayList<CheckBox>()
    var isLongButtonClick=false
    var trashBin: MenuItem?=null
    var isNote=false

      private val requiredPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    lateinit var buttonColorList:List<ColorData>

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_YellowNote)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        setContentView(binding.root)
        println("onCreate")
        // toolbar 설정
        val toolbar = binding.introToolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // toolbar 제목 표시 유무
        linearLayout=binding.linearLayout
        myDao = MyDatabase.getDatabase(this).getMyDao()
        //val allStudents = myDao.getAllPenData()
        setMyDatabase()
        requestAudioPermission()
        //setButtonHandlers()
        // 펜 정보 담고 있는 뷰모델 생성
        penInfo = ViewModelProvider(this)[PenInfo::class.java]
    }

    private fun requestAudioPermission() {
        requestPermissions(requiredPermissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    fun setMyDatabase(){
        CoroutineScope(Dispatchers.Main).launch {
            if(myDao.getPenDataCount()==0){
                myDao.insertPenData(PenData("PEN",10f,Color.BLACK,true))
                myDao.insertPenData(PenData("ERASER",15f, null,false))
                myDao.insertPenData(PenData("TEXT",10f,Color.BLACK,false))
                myDao.insertPenData(PenData("CLIPPING",5f,Color.GRAY,false))
//                myDao.insertPenData(PenData("SHAPE",10f, Color.BLACK,100,false))
            }
            else{
                getActivePenData()
            }

            if(myDao.getColorDataCount()==0){
                myDao.insertColorData(ColorData("ColorButton1",-16777216))
                myDao.insertColorData(ColorData("ColorButton2",-65536))
                myDao.insertColorData(ColorData("ColorButton3",-256))
                myDao.insertColorData(ColorData("ColorButton4",-16711936))
                myDao.insertColorData(ColorData("ColorButton5", -16776961))
                myDao.insertColorData(ColorData("ColorButton6",-7829368))
            }
            buttonColorList = myDao.getColorData()
        }
    }

    fun getActivePenData(){
        var activePenData = myDao.getActivePenData()
        System.out.println("${activePenData[0]}")

        if(activePenData[0].color!=null){
            penInfo.setPenColor(activePenData[0].color!!)
        }
        penInfo.setPenWidth(activePenData[0].width!!)
        penInfo.setPenMode(PenModes.indexOf(activePenData[0].mode))
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onStart() {
        super.onStart()
        try {
            getActivePenData()
        } catch (e: Exception) {

        }

        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")

        buttonColorList = myDao.getColorData()
        makeNoteList()
    }
    fun makeNoteList(){
        noteList=myDao.getAllNoteData()
        linearLayout.removeAllViews()
        deleteButtons.clear()
        val display=windowManager.defaultDisplay
        val size= Point()
        display.getRealSize(size)
        val width=size.x
        val divValue=width/230

        var innerLayout:LinearLayout?=null
        for((i, noteInfo) in noteList.withIndex()) {
            if (i % divValue == 0) {
                innerLayout = LinearLayout(this)
                val param = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                innerLayout.layoutParams = param
                innerLayout.orientation=LinearLayout.HORIZONTAL
                linearLayout.addView(innerLayout)
            }

            val noteLayout=LinearLayout(this)
            val noteParams= LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            noteLayout.gravity=Gravity.CENTER
            noteLayout.layoutParams=noteParams
            noteLayout.orientation=LinearLayout.VERTICAL

            val noteView = RelativeLayout(this)
            val noteViewName=TextView(this)
            val deleteButton=CheckBox(this)

            val deleteParams= RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)
            val textParams=RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)

            deleteParams.addRule(RelativeLayout.CENTER_IN_PARENT,RelativeLayout.TRUE)
            deleteButton.layoutParams=deleteParams
            deleteButton.id = i
            deleteButton.visibility= View.INVISIBLE
            deleteButtons.add(deleteButton)
            noteView.addView(deleteButton)

            noteViewName.layoutParams=textParams
            noteViewName.gravity=Gravity.CENTER
            noteViewName.textAlignment=View.TEXT_ALIGNMENT_CENTER
            noteViewName.text=noteInfo.noteName
            noteViewName.ellipsize= TextUtils.TruncateAt.END
            noteViewName.setEms(6)
            noteViewName.maxLines=2

            noteView.background= resources.getDrawable(R.drawable.ic_note)
            val noteParam= RelativeLayout.LayoutParams(230, 230)
            noteView.layoutParams=noteParam
            noteView.setOnLongClickListener{
                println("long click")
                isLongButtonClick=true
                trashBin?.isVisible=true
                for(b in deleteButtons)
                    b.visibility=View.VISIBLE
                true
            }
            noteView.setOnClickListener {
                if (!isLongButtonClick) {
                    if (File(noteInfo.recordFileLocation).isFile) {
                        if (File(noteInfo.recordFileLocation).canRead()) {
                            startActivity(
                                Intent(this, PdfActivity()::class.java)
                                    .putExtra("filePath", noteInfo.recordFileLocation)
                                    .putExtra("lastPage", noteInfo.lastPageNo)
                                    .putExtra("noteName",noteInfo.noteName)
                                    .putExtra("penColor", penInfo.getPenColor()) // penInfo 정보 보내기
                                    .putExtra("penWidth", penInfo.getPenWidth())
                                    .putExtra("penMode", penInfo.getPenMode())
                                    .putExtra("ColorButton1", buttonColorList[0].color)
                                    .putExtra("ColorButton2", buttonColorList[1].color)
                                    .putExtra("ColorButton3", buttonColorList[2].color)
                                    .putExtra("ColorButton4", buttonColorList[3].color)
                                    .putExtra("ColorButton5", buttonColorList[4].color)
                                    .putExtra("ColorButton6", buttonColorList[5].color)
                            )
                        }
                    }
                }
            }
            noteLayout.addView(noteView)
            noteLayout.addView(noteViewName)
            innerLayout?.addView(noteLayout)
        }
    }

    // toolbar에 menu item 넣기
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.intro_toolbar, menu)
        trashBin=menu?.findItem(R.id.deletenote)
        trashBin?.isVisible=false
        return true
    }

    //item 버튼 클릭 했을 때
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.deletenote->{
                println("삭제 메뉴 클릭")
                for(b in deleteButtons){
                    if(b.isChecked){
                        val note=noteList[b.id]
                        runBlocking {
                            myDao.deleteNoteData(note)
                        }
                    }

                }
                isLongButtonClick=false
                trashBin?.isVisible=false
                makeNoteList()
                return true
            }
//            R.id.selectNote -> { // 필기 선택 메뉴 누른 경우
//                println("선택 메뉴 클릭")
//                return true
//            }
            R.id.addNote -> { // 필기 추가 메뉴 누른 경우
                println("플러스 메뉴 클릭")
                return true
            }
            R.id.addMethodFolder -> { // 필기 추가>파일 메뉴 누른 경우
                println("파일 클릭")
                checkPermissions(permissions) // 저장소 권한 확인
                return true
            }
            R.id.addMethodTemplate-> { // 필기 추가>노트 메뉴 누른 경우
                println("노트 클릭")
                isNote=true
//                checkPermissions(permissions)
                val dialog = PlayGroundDialog(this,this)
                dialog.show()
                isNote=false

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // 권한 확인
    private fun checkPermissions(permissions:Array<String>) {
        var targetList = arrayListOf<String>() // 권한 없는 항목들

        for(i in 0..permissions.size-1){
            var curPermission = permissions[i]
            var permissionCheck = ContextCompat.checkSelfPermission(this, curPermission)
            if(permissionCheck == PackageManager.PERMISSION_GRANTED) { // 권한이 이미 부여된 경우
                System.out.println("***** 저장소 권한 있음 *****")
                fileChooser() // 파일 선택창 띄우기
                return
            }
            else{ // 권한을 부여받지 못한 경우
                System.out.println("***** 저장소 권한 없음 *****")
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,curPermission)) {
                    System.out.println("***** 저장소 권한 설명 필요 *****")
                }
                targetList.add(curPermission) // 권한 없는 항목들에 포함시키기
            }
        }

        val targets = arrayOfNulls<String>(targetList.size) // 권한 요청할 항목들
        targetList.toArray(targets)

        ActivityCompat.requestPermissions(this, targets,101) // 위험 권한 부여 요청
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val audioRequestPermissionGranted =
            requestCode == REQUEST_RECORD_AUDIO_PERMISSION &&
                    grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        //권한이 부여되지않으면 어플 종료
        if (!audioRequestPermissionGranted) {
            finish()
        }
        when(requestCode){ // 요청 코드 맞는지 확인

            101 -> { // 사용자 권한 수락했는지 여부 확인
                if(grantResults.size>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    System.out.println("***** 권한 승인 *****")
                    fileChooser() // 파일 선택창 띄우기
                }
                else
                    System.out.println("***** 권한 거부 *****")
            }
        }
    }

    // 저장소 파일 선택창 띄우기
    fun fileChooser(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            isKitKat = true
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/pdf"
            startForResult.launch(Intent.createChooser(intent, "Select file"))
        } else {
            isKitKat = false
            val intent = Intent()
            intent.type = "application/pdf"
            intent.action = Intent.ACTION_GET_CONTENT
            startForResult.launch(Intent.createChooser(intent, "Select file"))
        }
    }

    val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result:ActivityResult->
        if(result.resultCode== AppCompatActivity.RESULT_OK){ // 파일 선택 완료 시
            val data: Intent? = result.data
            if (data != null) {
                val uri = data.data
                if (uri != null) {
                    System.out.println("Uri from onActivityResult: $uri")
                    var filePath = getPath(this,uri)
                    System.out.println("filePath = "+filePath)
                    if(filePath!=null){
                        System.out.println("File(filePath).isFile = "+File(filePath).isFile)
                        System.out.println("File(filePath).canRead = "+File(filePath).canRead())

                        if(File(filePath).isFile) {
                            if (File(filePath).canRead()){
                                startActivity(
                                    Intent(this, PdfActivity()::class.java)
                                        .putExtra("filePath", filePath)
                                        .putExtra("penColor",penInfo.getPenColor()) // penInfo 정보 보내기
                                        .putExtra("penWidth",penInfo.getPenWidth())
                                        .putExtra("penMode",penInfo.getPenMode())
                                        .putExtra("ColorButton1", buttonColorList[0].color)
                                        .putExtra("ColorButton2", buttonColorList[1].color)
                                        .putExtra("ColorButton3", buttonColorList[2].color)
                                        .putExtra("ColorButton4", buttonColorList[3].color)
                                        .putExtra("ColorButton5", buttonColorList[4].color)
                                        .putExtra("ColorButton6", buttonColorList[5].color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("Range")
    fun getPath(context: Context, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) { //////////// 해결 완
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    System.out.println(Environment.getExternalStorageDirectory().toString() + "/" + split[1])
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                val displayName:String
                val cursor: Cursor? = contentResolver.query( uri, null, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst())
                        displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
                val id = DocumentsContract.getDocumentId(uri)
                val split = id.split(":").toTypedArray()
                System.out.println(id)
                if (id.startsWith("msf")) {
                    return null
                }
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )
//                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.getScheme(), ignoreCase = true)) {
//            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.getScheme(), ignoreCase = true)) {
            return uri.getPath()
        }
        return null
    }

    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.getAuthority()
    }

    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.getAuthority()
    }

    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.getAuthority()
    }
    companion object {
        const val RECORDER_SAMPLERATE = 44100
        const val RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_IN_MONO
        const val RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
        private const val REQUEST_RECORD_AUDIO_PERMISSION =201
    }

    fun openPDF(filePath:String){
        if(File(filePath).isFile) {
            if (File(filePath).canRead()){
                startActivity(
                    Intent(this, PdfActivity()::class.java)
                        .putExtra("filePath", filePath)
                        .putExtra("penColor",penInfo.getPenColor()) // penInfo 정보 보내기
                        .putExtra("penWidth",penInfo.getPenWidth())
                        .putExtra("penMode",penInfo.getPenMode())
                        .putExtra("ColorButton1", buttonColorList[0].color)
                        .putExtra("ColorButton2", buttonColorList[1].color)
                        .putExtra("ColorButton3", buttonColorList[2].color)
                        .putExtra("ColorButton4", buttonColorList[3].color)
                        .putExtra("ColorButton5", buttonColorList[4].color)
                        .putExtra("ColorButton6", buttonColorList[5].color)
                )
            }
        }
    }
}

