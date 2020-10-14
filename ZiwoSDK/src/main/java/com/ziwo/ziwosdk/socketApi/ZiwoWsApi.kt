package com.ziwo.ziwosdk.socketApi

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ziwo.ziwosdk.Ziwo
import com.ziwo.ziwosdk.httpApi.AgentStatus
import com.ziwo.ziwosdk.SessionIdNotSetException
import com.ziwo.ziwosdk.verto.WebSocketStatus
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ZiwoWsApi  @Inject constructor(
    var context: Context,
    var ziwoMain: Ziwo
)  : WebSocketListener()  {

    val TAG = "ZiwoApiSocket"
    var socket: Socket? = null;
    var gson = Gson()

    // login
    private var callcenter: String? = null
    private var accessToken: String? = null

    // ws
    var socketHandler : ZiwoWsHandlerInterface? = null
    var webSocketStatus : WebSocketStatus =  WebSocketStatus.Disconnected
        set(value) {
            socketHandler?.onApiSocketStatusChange(value)
            Log.i(TAG, "status $webSocketStatus")

            if (
                value == WebSocketStatus.Failed
                || value == WebSocketStatus.Disconnected
            ){
                socketHandler?.onAgentStatusChange(AgentStatus.LoggedOut)
            }
            field = value
        }

    /**  Opens the socket and set login paremeters [callcenter] [accessToken] */
    fun login(callcenter: String, accessToken: String) {

        this.callcenter = callcenter
        this.accessToken =  accessToken

        val opts: IO.Options = IO.Options()
        opts.query = "access_token=$accessToken"
        opts.path = "/socket"
        opts.reconnection = true
        opts.transports = arrayOf("websocket")

        try {
            socket = IO.socket("https://$callcenter-api.aswat.co", opts);
            socket
                ?.on(Socket.EVENT_CONNECTING) {
                    onReconnect()
                }
                ?.on(Socket.EVENT_RECONNECT) {
                    onReconnect()
                }
                ?.on(Socket.EVENT_CONNECT) {
                    onConnect()
                }
                ?.on(Socket.EVENT_ERROR) {
                    for (element in it) {
                        Log.d(TAG, "EVENT_ERROR $element")
                    }
                    webSocketStatus = WebSocketStatus.Failed
                }
                ?.on(Socket.EVENT_RECONNECT_ERROR) {
                    for (element in it) {
                        Log.d(TAG, "EVENT_ERROR $element")
                    }
                    webSocketStatus = WebSocketStatus.Failed
                }
                ?.on(Socket.EVENT_DISCONNECT) {
                    webSocketStatus = WebSocketStatus.Disconnected
                }
                ?.on(Socket.EVENT_RECONNECT_FAILED) {
                    webSocketStatus = WebSocketStatus.Disconnected
                }
            socket?.connect()
        } catch (ex: Exception){
            webSocketStatus = WebSocketStatus.Failed
            Log.d(TAG, "opensocketFailed ${ex.toString()}")
        }


    }

    private fun onReconnect() {
        webSocketStatus = WebSocketStatus.Retrying
        socketHandler?.onAgentStatusChange(AgentStatus.Retrying)
    }

    private fun onConnect(){

        webSocketStatus = WebSocketStatus.Ready

        // listeners
        socket?.on(WsApiRoutes.GetLiveStatus) {
            for (element in it) {
                Log.d(TAG, "${WsApiRoutes.GetLiveStatus} ${element::class.qualifiedName} $element ")
                val messageType = object : TypeToken<WsApiRes<GetLiveStatus>>(){}.type
                val message = gson.fromJson(element.toString(), messageType ) as WsApiRes<GetLiveStatus>
                message.content.status?.let { status -> socketHandler?.onAgentStatusChange(status) }
            }
        }
        socket?.on(WsApiRoutes.GetProfile) {
            for (element in it) {
                Log.d(TAG, "${WsApiRoutes.GetProfile} $element")
            }
        }

        // TODO custom subscribe to different events
        // subscribing
        socket?.emit("subscribe", WsApiRoutes.GetLiveStatus)
        //socket?.emit(WsApiRoutes.GetLiveStatus, {})

    }

    /**
     * reconnect the websocket, throws error if haven't loggedin in main
     */
    public fun reconnect(){
        socket?.close()
        if ( this.callcenter.isNullOrEmpty() || this.accessToken.isNullOrEmpty()){
            throw SessionIdNotSetException("login not initialized")
        }
        login(this.callcenter!!, this.accessToken!!)
    }

    public fun disconnect(){
        socket?.close()
    }





}