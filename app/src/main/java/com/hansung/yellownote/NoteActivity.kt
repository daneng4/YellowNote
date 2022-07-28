package com.hansung.yellownote

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import com.hansung.yellownote.databinding.ActivityNoteBinding
import java.io.File


class NoteActivity : AppCompatActivity() {
    private lateinit var binding : ActivityNoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var intent = intent
        var uriString = intent.getStringExtra("uri")
        var uri = Uri.parse(uriString)

        System.out.println("ddddddddddddd"+getFileExtension(this,uri))

//        var file = File(uri.toString())
//        System.out.println(getRealPathFromURI(this,uri))
//        System.out.println(getFileName(uri))
//        pdf_view_pager.adapter = PageAdaptor()
//        val targetFile = uri.toFile()
//        pdfReader = PdfReader(targetFile).apply {
//            (pdf_view_pager.adapter as PageAdaptor).setupPdfRenderer(this)
//        }
//        TabLayoutMediator(pdf_page_tab, pdf_view_pager) { tab, position ->
//            tab.text = (position + 1).toString()
//        }.attach()

    }

    private fun getFileExtension(context: Context, uri: Uri): String? =
        if (uri.scheme == ContentResolver.SCHEME_CONTENT)
            MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))
        else uri.path?.let { MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(it)).toString())
        }

    fun getRealPathFromURI(context: Context, contentUri: Uri?): String? {
        var cursor: Cursor? = null
        val proj = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        cursor = contentUri?.let { context.getContentResolver().query(it, proj, null, null, null) }
        var path=""
        if(cursor!=null){
            path = cursor.getString(0)
            cursor.close()
        }
        return path
    }

    @SuppressLint("Range")
    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }


}