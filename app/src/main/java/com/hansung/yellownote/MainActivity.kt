package com.hansung.yellownote

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hansung.yellownote.databinding.ActivityMainBinding
import com.hansung.yellownote.drawing.PdfActivity
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var permissions = arrayOf(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO)
    var isKitKat = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_YellowNote)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // toolbar 설정
        var toolbar = binding.introToolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // toolbar 제목 표시 유무
    }

    // toolbar에 menu item 넣기
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.intro_toolbar, menu)
        return true
    }

    //item 버튼 클릭 했을 때
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.selectNote -> { // 필기 선택 메뉴 누른 경우
                println("선택 메뉴 클릭")
                return true
            }
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
    private fun fileChooser(){
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
                System.out.println("data = "+data)
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
//                                    Intent(this, NoteActivity::class.java).
//                                    putExtra("filePath", filePath))
                                    Intent(this, PdfActivity::class.java).
                                    putExtra("filePath", filePath))
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
}

