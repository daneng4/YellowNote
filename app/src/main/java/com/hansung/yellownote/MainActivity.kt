package com.hansung.yellownote

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.hansung.yellownote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_YellowNote)
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
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
//            android.R.id.home -> {
//                return true
//            }
            R.id.selectNote -> {
                //노트 선택 버튼 누른 경우
                println("Click selectNote menu btn")
                return true
            }
            R.id.addNote -> {
                //노트 추가 버튼 누른 경우
                println("Click addNote menu btn")
                val intent = Intent(this, NoteActivity::class.java)
                intent.putExtra("filePath",this.cacheDir.absolutePath+"/wadifoo.pdf")
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}