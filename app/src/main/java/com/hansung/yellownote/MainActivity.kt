package com.hansung.yellownote

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
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

    // 저장소 파일 선택창 띄우기
    private fun fileChooser(){
        val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
        var uri = Uri.parse(Environment.getExternalStorageDirectory().getPath())
        fileIntent.setDataAndType(uri, "application/pdf/*");

        startActivity(fileIntent)
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
                // 저장소 권한 확인
                checkPermissions(permissions)
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
        var targetList = arrayListOf<String>()

        for(i in 0..permissions.size-1){
            var curPermission = permissions[i]
            var permissionCheck = ContextCompat.checkSelfPermission(this, curPermission)
            if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
                System.out.println("***** 저장소 권한 있음 *****")
                fileChooser()///////////////////
                return
            }
            else{
                System.out.println("***** 저장소 권한 없음 *****")
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,curPermission)) {
                    System.out.println("***** 저장소 권한 설명 필요 *****")
                }
                targetList.add(curPermission)
            }
        }

        // 권한 부여 요청할 target들
        val targets = arrayOfNulls<String>(targetList.size)
        targetList.toArray(targets)

        ActivityCompat.requestPermissions(this, targets,101) // 위험 권한 부여 요청
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){ // 요청 코드 맞는지 확인
            101 -> { // 사용자 권한 수락했는지 여부 확인
                if(grantResults.size>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    System.out.println("***** 권한 승인 *****")
                    fileChooser()
                }
                else
                    System.out.println("***** 권한 거부 *****")
            }
        }
    }
}