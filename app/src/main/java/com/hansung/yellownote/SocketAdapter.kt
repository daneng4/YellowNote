package com.hansung.yellownote

import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.net.Socket
import java.nio.file.Files
import kotlin.io.path.Path

class SocketAdapter {
    // 소켓통신에 필요한것
    private val html = ""
    private var mHandler: Handler? = null
    private var socket: Socket? = null
    private var dos: DataOutputStream? = null
    private var dis: DataInputStream? = null
    private val ip = "192.168.255.161" // IP 번호
    private val port = 9999 // port 번호

    // 로그인 정보 db에 넣어주고 연결시켜야 함.
    @RequiresApi(33)
    fun connect(WavFilePath: String)  {
        System.out.println(WavFilePath)
        val path = Path(WavFilePath)
        val input = FileInputStream(WavFilePath)
        val audiostream = DataInputStream(input)
        val size = Files.size(path)
        System.out.println(size)
        mHandler = Handler()
        Log.w("connect", "연결 하는중")
        // 받아오는거
        val checkUpdate: Thread = object : Thread() {
            override fun run() {
                // 서버 접속
                try {
                    socket = Socket(ip, port)
                    Log.w("서버 접속됨", "서버 접속됨")
                } catch (e1: IOException) {
                    Log.w("서버접속못함", "서버접속못함")
                    e1.printStackTrace()
                }
                try {
                    dos = DataOutputStream(socket!!.getOutputStream()) // output에 보낼꺼 넣음
                    dis = DataInputStream(socket!!.getInputStream()) // input에 받을꺼 넣어짐

                    dos!!.writeUTF(size.toString())
                    dos!!.flush()
                    dos!!.write(audiostream.readBytes())
                    dos!!.flush()


                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.w("버퍼", "버퍼생성 잘못됨")
                }
                Log.w("버퍼", "버퍼생성 잘됨")

                // 서버에서 계속 받아옴 - 한번은 문자, 한번은 숫자를 읽음. 순서 맞춰줘야 함.
                /* try {
                     while (true) {
                         var returnFile = dis!!.readAllBytes()
                     }
                 } catch (e: Exception) {
                 }*/
            }
        }
        // 소켓 접속 시도, 버퍼생성
        checkUpdate.start()
    }
}