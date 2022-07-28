package com.hansung.yellownote

import android.R.attr
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
            val data = result.data // uri 값
            if (data != null) {

                //https://stackoverflow.com/questions/51101608/how-to-get-the-folder-path-using-intent-on-android
                var FilePath = data.data?.path;
                var FileName = data.data?.lastPathSegment;
//                var lastPos = (FilePath?.size) - (FileName?.size)
//                var Folder = FilePath?.substring(0, lastPos);

                println("Full Path:" + FilePath);
//                println("Folder:" + Folder);
                println("File Name:" + FileName)

//                val FilePath: String? = data.data?.path
//                val FileName: String? = data.data?.lastPathSegment
//                val lastPos = (FilePath?.length ?: 0) - (FileName?.length ?: 0)
//                val Folder = FilePath?.substring(0, lastPos)
//
//                println("Full Path: \n$FilePath\n")
//                println("Folder: \n$Folder\n")
//                println("File Name: \n$FileName\n")

                startActivity(Intent(this, NoteActivity::class.java).putExtra("uri",data.toString()))
            }
        }
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