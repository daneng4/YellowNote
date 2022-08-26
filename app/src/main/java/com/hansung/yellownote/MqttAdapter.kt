package com.hansung.yellownote

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import java.io.Serializable

class MqttAdapter{
    private lateinit var client: MqttClient
    constructor(){
        try{
            client= MqttClient("tcp://223.194.132.247:1883",MqttClient.generateClientId(),null)
            if(!client.isConnected){
                client.connect()
            }
            client.subscribe("result")
            client.setCallback(object:MqttCallback{
                override fun connectionLost(cause: Throwable?) {
                    Log.d("MqttService","Connection Lost")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if(topic=="result"){

                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MqttService","Delivery Complete")
                }
            })
        }catch (e:MqttException){
            e.printStackTrace()
        }
    }
    fun sendPixelMessage(drawings:ByteArray){
        try{
            System.out.println("sendPixels")
            if(!client.isConnected)
                client.connect()
            client.publish("easyocr/hangul",MqttMessage(drawings))
        }catch (e:MqttException){
            e.printStackTrace()
        }
    }
    fun sendImageSizeMessage(width:Int){
        val message=width.toString()
        println(width)
        println(message)
        println(message.toByteArray())
        try{
            if(!client.isConnected)
                client.connect()
            client.publish("imagesize",MqttMessage(message.toByteArray()))
        }catch(e:MqttException){
            e.printStackTrace()
        }
    }
}