package com.ziwo.ziwosdk.utils.ziwoSdk.verto

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ziwo.ziwosdk.Call
import com.ziwo.ziwosdk.Ziwo
import com.ziwo.ziwosdk.httpApi.ZiwoApi
import com.ziwo.ziwosdk.verto.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.webrtc.SessionDescription
import java.util.*
import kotlin.concurrent.schedule


class VertoWs(
    var context: Context,
    var ziwoMain: Ziwo
)  : WebSocketListener()  {

    val TAG = "ZiwoVertoSocket"
    var client: WebSocket? = null;
    var gson = Gson()

    // login
    private lateinit var callcenter: String
    private lateinit var userName: String;
    private lateinit var userPassword: String;
    lateinit var sessionId: String

    // ws
    var webSocketCommandsSender= VertoCommandsSender(this)
    var webSocketStatus : WebSocketStatus =  WebSocketStatus.Disconnected
        set(value) {
            vertoHandler?.onVertoSocketStatusChange(value)
            field = value
        }
    var callsList = sortedMapOf<String, Call>()
    var manualMessagesList = mutableListOf<ManualVertoMessage>()

    // rxJava
    var vertoHandler: VertoHandlerInterface? = null
    var attachJob : Job? = null
    var disconnectedJob: Job? = null

    /**  returns login string used in various send command messages  */
    fun getLogin(): String {
        return "$userName@$callcenter.aswat.co"
    }
    /**   set login paremeters [userName] [userPassword] */
    fun setLoginCredentials(callcenter: String, userName: String, userPassword: String, sessionId: String) {

        this.callcenter = callcenter
        this.userName = userName;
        this.userPassword = userPassword;
        this.sessionId = sessionId
    }

    /**  Opens the socket and set login paremeters [userName] [userPassword] */
    fun login() {
        ziwoMain.logger(TAG, "login")
        ziwoMain.logger(TAG, "login uuid $sessionId")

        val request = okhttp3.Request.Builder().url("wss://$callcenter-api.aswat.co:8082").build()
        val listener = this;

        client = OkHttpClient().newWebSocket(request, listener )

        ziwoMain.logger(TAG, "login 5alas")

    }

    /**
     * pass verto socket messages manually, useful for notifications integrations such as firebase
     * the messages will be passed into [onMessage] when socket is set to ready
     */
    fun manualInsertMessage(message: ManualVertoMessage){

        if (webSocketStatus == WebSocketStatus.Ready){
            client?.let{
                onMessage( it , message.content)
            }
        } else {
            manualMessagesList.add(message)
        }

    }

    override fun onOpen(webSocket: WebSocket, response: Response) {

        // update socket status
        webSocketStatus = WebSocketStatus.PendingLogin

        /**
         * --This is very important step to recover using verto.attach:
         * the request will return authentication failed but it recovering prevoius
         * call will will not work without it
         */
        webSocketCommandsSender.send(
            VertoEvent.LOGIN,
            params = VertoMessageLoginParams(
                sessid = sessionId,
            )
        )

        // maybe add delay here

        // send the actual login
        webSocketCommandsSender.send(
            VertoEvent.LOGIN,
            params = VertoMessageLoginParams(
                login = userName,
                passwd = userPassword,
                sessid = sessionId,
            )
        )

    }

    override fun onMessage(webSocket: WebSocket, text: String) {

        // parse event type to know which handler to call
        val messageEvent = gson.fromJson(text, VertoMessage<Nothing>()::class.java )
        ziwoMain.logger( TAG, "\n $text")

        /** check which handler */

        // Verto Method
        if (messageEvent.method != null) {
            parseVertoEvent(messageEvent.method, text)
            return
        }

        // Verto Result
        if (messageEvent.result != null) {
            val messageType = object : TypeToken<VertoMessage<VertoResult>>(){}.type
            val message = gson.fromJson(text, messageType ) as VertoMessage<VertoResult>

            // detect when call end
            if ( messageEvent.result.message == VertoNotificationMessage.CallEnded){
                callsList[message.result?.callID]?.destroy()
            }

            // detect hold and unhold
            if ( messageEvent.result.holdState == VertoNotificationHoldState.Held) {
                callsList[message.result?.callID]?.pushState(Call.Companion.ZiwoEventType.Held)
            }
            if ( messageEvent.result.holdState == VertoNotificationHoldState.Active) {
                callsList[message.result?.callID]?.pushState(Call.Companion.ZiwoEventType.Active)
            }

        }


    }
    var onFailCounter = 0
    override fun onFailure( webSocket: WebSocket,  t: Throwable, response: Response?) {

        webSocket.cancel()
        myOnFailure()

    }
    fun myOnFailure(){
        client?.cancel()
        finishWebsocket()

        onFailCounter++
        webSocketStatus = WebSocketStatus.Failed

        Timer().schedule(5000) {
            if (webSocketStatus == WebSocketStatus.Failed ) {
                ziwoMain.logger(TAG, "onFailureCounter $onFailCounter")

                webSocketStatus = WebSocketStatus.Retrying
                login()
            }
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        webSocketStatus = WebSocketStatus.Closed
    }

    /** Code related to processing socket messages */
    fun parseVertoEvent(method: VertoEvent, rawSocketMessage: String){
        when (method) {

            /** Succesful Login */
            VertoEvent.ClientReady -> {

                //  send login command
                try {
                    GlobalScope.launch {
                        ziwoMain.ziwoApiClient.autoLogin()
                    }
                } catch (ex: java.lang.Exception){
                    myOnFailure()
                    ziwoMain.logger(TAG, "ziwo api autologin failed $ex")
                    ziwoMain.logger(TAG, ex)
                    return
                }

                // update socket status
                webSocketStatus = WebSocketStatus.Ready
                // val message = gson.fromJson(rawSocketMessage, VertoMessage<VertoMessageLoginParams>()::class.java )

                // run clean disconnected jobs
                disconnectedJob =  GlobalScope.launch {

                    delay(10000L) // wait 10 seconds to make sure the server wont reattach the calls
                    callsList.forEach { it->
                        val call = it.value
                        if(call.eventsArray.last().event == Call.Companion.ZiwoEventType.Disconnected){
                            call.hangup()
                        }
                    }

                }

                // process manual messages
                manualMessagesList.forEach { manualVertoMessage ->
                    client?.let{
                        onMessage( it , manualVertoMessage.content)
                    }
                }
                manualMessagesList.clear()

            }

            /** Incoming call */
            VertoEvent.Invite -> {

                val messageType = object : TypeToken<VertoMessage<VertoMessageInviteParams>>(){}.type
                val message = gson.fromJson(rawSocketMessage, messageType ) as VertoMessage<VertoMessageInviteParams>

                message.params!!
                val call = Call(
                    message.params.callID,
                    webSocketCommandsSender,
                    message.params.callerIdName,
                    getLogin(),
                    ZiwoApi.Enums.CallDirection.INBOUND,
                    Call.Companion.ZiwoEventType.Ringing,
                    mapOf("sdp" to message.params.sdp)
                )

                callsList[message.params.callID] = call
                vertoHandler?.callStarted( call )

            }

            /** Webrtc sdp Exchange info*/
            VertoEvent.Media -> {

                val messageType = object : TypeToken<VertoMessage<VertoMediaParams>>(){}.type
                val message = gson.fromJson(rawSocketMessage, messageType ) as VertoMessage<VertoMediaParams>

                try {
                        callsList[message.params?.callID]?.pushState(Call.Companion.ZiwoEventType.Early)
                        callsList[message.params?.callID]?.rtcPeerConnection?.setRemoteDescription(
                            MySdpObserver("Verto.Media_setRemoteSDp", callsList[message.params?.callID]!!, ziwoMain),
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                message.params?.sdp
                            )
                        )
                    ziwoMain.logger(TAG, "added remote description")
                } catch (ex: Exception) {
                    ziwoMain.logger(TAG, "$ex")
                }
            }

            /** Call end */
            VertoEvent.Bye -> {

                val messageType = object : TypeToken<VertoMessage<VertoByeParamsIncoming>>(){}.type
                val message = gson.fromJson(rawSocketMessage, messageType ) as VertoMessage<VertoByeParamsIncoming>
                callsList[message.params?.callID]?.destroy()
            }

            VertoEvent.Attach -> {

                attachJob?.cancel()
                attachJob = GlobalScope.launch {

                    val messageType =
                        object : TypeToken<VertoMessage<VertoMessageAttachParams>>() {}.type
                    val message = gson.fromJson(
                        rawSocketMessage,
                        messageType
                    ) as VertoMessage<VertoMessageAttachParams>

                    message.params!!

                    if (callsList[message.params.callID] != null) {
                        callsList[message.params.callID]?.let { call ->

                            call.rtcClose()
                            call.pushState(Call.Companion.ZiwoEventType.Recovering)
                            val newRtcCollection =
                                RTCPeerConnectionFactory(
                                    context,
                                    ziwoMain,
                                ).recover(
                                    webSocketCommandsSender,
                                    message.params.sdp,
                                    call
                                )


                            call.io = newRtcCollection.localStream
                            call.rtcPeerConnection = newRtcCollection.pc
                        }
                    } else {
                        webSocketCommandsSender.hangup(
                            message.params.callID,
                            "",
                            VertoByeReason.CALL_REJECTED
                        )
                    }

                }

            }

            /** TODO: Unknown */
            VertoEvent.Display -> {
                // TODO: update call params?
                val messageType = object : TypeToken<VertoMessage<VertoDisplayParams>>(){}.type
                val message = gson.fromJson(rawSocketMessage, messageType ) as VertoMessage<VertoDisplayParams>
                callsList[message.params?.callID]?.pushState(Call.Companion.ZiwoEventType.Active)
            }

            VertoEvent.Answer ->{
                // val messageType = object : TypeToken<VertoMessage<VertoMessageAnswerParams>>(){}.type
                // val message = gson.fromJson(rawSocketMessage, messageType ) as VertoMessage<VertoMessageAnswerParams>
            }

            else -> {}
        }
    }

    /**
     * shared logic between onclosed and failure
     */
    private fun finishWebsocket(){
        attachJob?.cancel()
        disconnectedJob?.cancel()
        callsList.forEach { ( _, callObject) ->

            val lastEvent = callObject.eventsArray.last().event
            if (
                lastEvent !== Call.Companion.ZiwoEventType.Disconnected
                && lastEvent !== Call.Companion.ZiwoEventType.Destroy
            ) {
//                callObject.rtcClose()
            }
        }
    }

    /**
     * put all other calls on hold
     */
    fun setActiveCall(selectedCallId: String?) {

        callsList.forEach { ( callId, callObject) ->

            // TODO improve logic with complex shit of recovering state
            if(
                callId !== selectedCallId &&
                (
                callObject.eventsArray.last().event == Call.Companion.ZiwoEventType.Active ||
                callObject.eventsArray.last().event == Call.Companion.ZiwoEventType.Answering
                )
            ){
                callObject.hold()
            } else {
                // TODO restore hold
            }

        }
    }

    fun logout() {
        callsList.forEach { ( callId, _) ->
            callsList[callId]?.destroy()
        }
        callsList.clear()
        client?.close(1001, "logged out")

    }

    fun reconnect(){
        // TODO: Check vars first
        logout()
        login()
    }

}