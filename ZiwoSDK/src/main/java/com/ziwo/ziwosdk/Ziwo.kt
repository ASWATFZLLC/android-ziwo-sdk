package com.ziwo.ziwosdk

import android.content.Context
import android.util.Log
import com.ziwo.ziwosdk.httpApi.ZiwoApi
import com.ziwo.ziwosdk.socketApi.ZiwoWsApi
import com.ziwo.ziwosdk.utils.ziwoSdk.verto.VertoWs

class Ziwo(appContext: Context, public val debug: Boolean = false)  {

    // class logic
    private val TAG = "ZiwoSDK"
    public var ziwoApiClient =  ZiwoApi(appContext,this)
    public var vertoWs = VertoWs(appContext, this)
    public var ziwoApiWs = ZiwoWsApi(appContext, this)

    fun logger(tag: String, message: String){
        if (debug){
            Log.d(tag, message)
        }
    }
    fun logger(tag: String, ex: Exception){
        if (debug){
            Log.d(tag, ex.stackTraceToString())
        }
    }

}