package com.hansung.yellownote

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.ArrayList

class AudioAdapter(context: Context, dataModels: ArrayList<Uri>?) : RecyclerView.Adapter<AudioAdapter.MyViewHolder>() {
    //리사이클러뷰에 넣을 데이터 리스트
    var dataModels: ArrayList<Uri>? = dataModels
    var context: Context = context

    // 리스너 객체 참조를 저장하는 변수
    private var listener: OnIconClickListener? = null

    /**
     * 커스텀 이벤트 리스너
     * 클릭이벤트를 Adapter에서 구현하기에 제약이 있기 때문에 Activity 에서 실행시키기 위해 커스텀 이벤트 리스너를 생성함.
     * 절차
     * 1.커스텀 리스너 인터페이스 정의
     * 2. 리스너 객체를 전달하는 메서드와 전달된 객체를 저장할변수 추가
     * 3. 아이템 클릭 이벤트 핸들러 메스드에서 리스너 객체 메서드 호출
     * 4. 액티비티에서 커스텀 리스너 객체 생성 및 전달(MainActivity.java 에서 audioAdapter.setOnItemClickListener() )
     */
    // 1.커스텀 리스너 인터페이스 정의
    interface OnIconClickListener {
        fun onItemClick(view: View?, position: Int)
    }

    // 2. 리스너 객체를 전달하는 메서드와 전달된 객체를 저장할변수 추가
    fun setOnItemClickListener(listener: OnIconClickListener?) {
        this.listener = listener
    }

    override fun getItemCount(): Int {
        //데이터 리스트의 크기를 전달해주어야 함
        return dataModels?.size!!
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        //자신이 만든 itemview를 inflate한 다음 뷰홀더 생성

        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.audio_list, parent, false)

        //생선된 뷰홀더를 리턴하여 onBindViewHolder에 전달한다.
        return MyViewHolder(view)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var audioBtn: ImageButton = itemView.findViewById(R.id.playBtn_itemAudio)
        var audioTitle: TextView = itemView.findViewById(R.id.audioTitle_itemAudio)

        init {
            audioBtn.setOnClickListener { view ->
                //3. 아이템 클릭 이벤트 핸들러 메스드에서 리스너 객체 메서드 호출
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    // 리스너 객체의 메서드 호출.
                    if (listener != null) {
                        listener!!.onItemClick(view, pos)
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val uriName: String = java.lang.String.valueOf(dataModels!!.get(position))
        val file = File(uriName)
        holder.audioTitle.text = file.name
    }

}