package com.ziwo.ziwosdk.socketApi

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ziwo.ziwosdk.SessionIdNotSetException
import com.ziwo.ziwosdk.Ziwo
import com.ziwo.ziwosdk.httpApi.AgentStatus
import com.ziwo.ziwosdk.verto.WebSocketStatus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.WebSocketListener
import org.json.JSONArray
import java.util.*


class ZiwoWsApi(
    var context: Context,
    var ziwoMain: Ziwo
) : WebSocketListener() {

    val TAG = "ZiwoApiSocket"
    var socket: Socket? = null;
    var gson = Gson()

    // login
    private var callcenter: String? = null
    private var accessToken: String? = null
    private var heartbeatTimer: Timer? = null
    var disposable:Disposable?=null


    // ws
    var socketHandler: ZiwoWsHandlerInterface? = null
    var webSocketStatus: WebSocketStatus = WebSocketStatus.Disconnected
        set(value) {
            socketHandler?.onApiSocketStatusChange(value)
            ziwoMain.logger(TAG, "status $webSocketStatus")

            if (
                value == WebSocketStatus.Failed
                || value == WebSocketStatus.Disconnected
            ) {
                socketHandler?.onAgentStatusChange(AgentStatus.LoggedOut, null)
            }
            field = value
        }

    /** set login paremeters [callcenter] [accessToken] */
    fun setLoginCredentials(callcenter: String, accessToken: String) {
        this.callcenter = callcenter
        this.accessToken = accessToken
        val opts: IO.Options = IO.Options()
        opts.timeout = 30000  // set a timeout of 30 seconds
        opts.query = "access_token=$accessToken"
        opts.path = "/socket"
        opts.reconnection = true
        opts.transports = arrayOf("websocket")
        socket = IO.socket("wss://$callcenter-api.aswat.co", opts);

    }

    /**  Opens the socket */
    fun login() {

        try {
            socket
                ?.on(Manager.EVENT_RECONNECT_ATTEMPT) {
                    onReconnect()
                    ziwoMain.logger(TAG, "Manager EVENT_RECONNECT_ATTEMPT")

                }
                ?.on(Manager.EVENT_RECONNECT) {
                    onConnect()
                    ziwoMain.logger(TAG, " Manager EVENT_RECONNECT")

                }
                ?.on(Manager.EVENT_RECONNECT_ERROR) {
                    for (element in it) {
                        ziwoMain.logger(TAG, "EVENT_ERROR $element")
                    }
                    webSocketStatus = WebSocketStatus.Failed
                }
                ?.on(Manager.EVENT_RECONNECT_FAILED) {
                    ziwoMain.logger(TAG, " Socket.EVENT_RECONNECT_FAILED")

                    webSocketStatus = WebSocketStatus.Disconnected
                }

                ?.on(Socket.EVENT_CONNECT) {
                    ziwoMain.logger(TAG, " Socket.EVENT_CONNECT")

                    onConnect()

                }
                ?.on(Socket.EVENT_CONNECT_ERROR) {
                    for (element in it) {
                        ziwoMain.logger(TAG, "EVENT_ERROR $element")
                    }
                    webSocketStatus = WebSocketStatus.Failed
                }
                ?.on(Socket.EVENT_DISCONNECT) {
                    ziwoMain.logger(TAG, " Socket.EVENT_DISCONNECT")
                    webSocketStatus = WebSocketStatus.Disconnected
                }


            socket?.io()?.on(Manager.EVENT_RECONNECT_ATTEMPT) {
                onReconnect()
                ziwoMain.logger(TAG, "Manager EVENT_RECONNECT_ATTEMPT")

            }
                ?.on(Manager.EVENT_RECONNECT) {
                    onConnect()
                    ziwoMain.logger(TAG, " Manager EVENT_RECONNECT")

                }
                ?.on(Manager.EVENT_RECONNECT_ERROR) {
                    for (element in it) {
                        ziwoMain.logger(TAG, "EVENT_ERROR $element")
                    }
                    webSocketStatus = WebSocketStatus.Failed
                }
                ?.on(Manager.EVENT_RECONNECT_FAILED) {
                    ziwoMain.logger(TAG, " Socket.EVENT_RECONNECT_FAILED")

                    webSocketStatus = WebSocketStatus.Disconnected
                }


            socket?.connect()
        } catch (e: java.net.SocketTimeoutException) {
            webSocketStatus = WebSocketStatus.Failed
            ziwoMain.logger(TAG, "Socket Timeout: ${e.message}");
            // handle the exception
        } catch (ex: Exception) {
            webSocketStatus = WebSocketStatus.Failed
            ziwoMain.logger(TAG, "opensocketFailed ${ex.toString()}")
        }

    }

    private fun onReconnect() {
        webSocketStatus = WebSocketStatus.Retrying
        socketHandler?.onAgentStatusChange(AgentStatus.Retrying, null)
    }

    private fun onConnect() {

        webSocketStatus = WebSocketStatus.Ready


        // listeners
        socket?.on(WsApiRoutes.GetLiveStatus) { args ->
            GlobalScope.launch(Dispatchers.IO) {
                for (element in args) {
                    try {
                        element?.let {
                            ziwoMain.logger(
                                TAG,
                                "${WsApiRoutes.GetLiveStatus} ${it::class.qualifiedName} $it"
                            )
                        }
                        val messageType = object : TypeToken<WsApiRes<GetLiveStatus>>() {}.type

                        if (!element.toString().isNullOrEmpty()) {
                            val message = gson.fromJson(
                                element.toString(),
                                messageType
                            ) as WsApiRes<GetLiveStatus>
                            ziwoMain.logger(TAG, "$message")

                            message.content?.let { content ->
                                content.status?.let { status ->
                                    socketHandler?.onAgentStatusChange(status, content.source)
                                }
                            }
                        } else {
                            ziwoMain.logger(TAG, "Expected JSON Object but found: $element")
                        }
                    } catch (e: Exception) {
                        ziwoMain.logger(
                            TAG,
                            "Error processing ${WsApiRoutes.GetLiveStatus} message: $e"
                        )
                    }
                }
            }
        }

        socket?.on(WsApiRoutes.GetProfile) {
            for (element in it) {
                ziwoMain.logger(TAG, "${WsApiRoutes.GetProfile} $element")
            }
        }


        socket?.emit("subscribe", WsApiRoutes.GetLiveStatus)
        socket?.emit(WsApiRoutes.GetLiveStatus, {})
        startHeartbeat()
    }


    /**
     * reconnect the websocket, throws error if haven't loggedin in main
     */
    public fun reconnect(){
        socket?.close()
        if ( this.callcenter.isNullOrEmpty() || this.accessToken.isNullOrEmpty()){
            throw SessionIdNotSetException("login not initialized")
        }
        if (!callcenter.isNullOrEmpty()&&!accessToken.isNullOrEmpty())
            setLoginCredentials(callcenter!!, accessToken!!)
        login()
    }

public fun disconnect() {
    heartbeatTimer?.cancel();
    disposable?.dispose()
    socket?.close()
    webSocketStatus = WebSocketStatus.Disconnected
}

private fun startHeartbeat() {
    heartbeatTimer = Timer()
    heartbeatTimer?.scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
            if (socket!!.connected()) {
                socket!!.emit("heartbeat", "ping")
                ziwoMain.logger(TAG, "heartbeat sent")

            }
        }
    }, 0, 10000) // Send heartbeat every 10 seconds
}



}