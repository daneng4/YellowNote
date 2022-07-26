package com.hansung.yellownote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.hansung.yellownote.databinding.ActivityNoteBinding

class NoteActivity : AppCompatActivity() {
    private lateinit var binding : ActivityNoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}