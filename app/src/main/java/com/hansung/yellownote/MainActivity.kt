package com.hansung.yellownote

import android.R.attr
import android.app.Activity
import android.content.ContentUris
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
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hansung.yellownote.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

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

    val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result:ActivityResult->
        if(result.resultCode== RESULT_OK){ // 파일 선택 완료 시
            val data: Intent? = result.data // uri 값
            if (data != null) {
                println(data)
                val uri=data.data
                println(uri!!.path)
                val path=getPath(uri)
                println(path)


                startActivity(Intent(this, NoteActivity::class.java).putExtra("uri",data.toString()))
            }
        }
    }
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    fun getPath(uri:Uri):String? {

        val isKitKat:Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(this, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri);
                val split = docId.split(":")
                val type = split[0];

                if ("primary".equals(type,ignoreCase = false)) {
                    return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), id.toLong())

                return getDataColumn(this, contentUri, null, null)
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]

                var contentUri: Uri? = null;
                when (type){
                    "image"-> {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    }
                    "video" -> {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    }
                    "audio"-> {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                }

                val selection = "_id=?";
                val selectionArgs = arrayOf(
                    split[1]
                )

                return getDataColumn(this, contentUri!!, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(this, uri, null, null)
        }
        // File
        else if ("file".equals(uri.scheme,ignoreCase = true)) {
            return uri.path!!
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private fun getDataColumn(context: Context, uri:Uri , selection:String? ,
                      selectionArgs:Array<String>?):String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs,
                null);
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            cursor?.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri:Uri):Boolean {
        return "com.android.externalstorage.documents"==(uri.authority);
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri:Uri):Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri:Uri ):Boolean {
        return "com.android.providers.media.documents"==(uri.authority);
    }

    // 저장소 파일 선택창 띄우기
    private fun fileChooser(){
        val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
//        var uri = Uri.parse(Environment.getExternalStorageDirectory().getPath())
//        fileIntent.setDataAndType(uri, "application/pdf/*");
//        var path = Environment.getExternalStorageDirectory().getPath()
        fileIntent.setType("application/*");

//        startActivity(fileIntent)

        startForResult.launch(fileIntent)

//        val testLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if(result.resultCode == RESULT_CODE) {
//                // Got data from other activity and process that data
//                Log.e("${result.data}")
//            }
//        }
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
}