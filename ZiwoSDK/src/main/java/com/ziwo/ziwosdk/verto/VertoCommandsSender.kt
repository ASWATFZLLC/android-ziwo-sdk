package com.ziwo.agent.utils.ziwoSdk.verto

import android.util.Log
import com.google.gson.Gson
import com.ziwo.ziwosdk.Call
import com.ziwo.ziwosdk.httpApi.ZiwoApi
import com.ziwo.ziwosdk.verto.*
import java.util.*


class VertoCommandsSender
    ( var vertoWs: VertoWs)
{

    val VERTO_TAG = "[ZIWO SDK] WebSocketCommandsSender "
    var messageId = 0

    val gson = Gson()

    fun <paramsType> send(method: VertoEvent, params: paramsType? = null) {

        messageId++
        val message = gson.toJson(
            VertoMessage<paramsType>(
            method = method,
            params = params,
            id = messageId
        )
        )
        Log.d(VERTO_TAG, message)
        vertoWs.client?.send(message )

    }

    fun startCall(phoneNumber: String) {

        // TODO do audio permission check

        val callId = UUID.randomUUID().toString()

        val call =  Call(
            callId,
            this,
            phoneNumber,
            vertoWs.getLogin(),
            ZiwoApi.Enums.CallDirection.OUTBOUND,
            Call.Companion.ZiwoEventType.Requesting
        )

        val rtcCollection = RTCPeerConnectionFactory(
            vertoWs.context
        ).outbound(
            this,
            call,
        )

        call.rtcPeerConnection = rtcCollection.pc
        call.io = rtcCollection.localStream



        vertoWs.callsList[call.callId] = call

        vertoWs.vertoHandler?.callStarted(call)

    }

    fun hangup(callId: String, phoneNumber: String, reason: VertoByeReason = VertoByeReason.NORMAL_CLEARING) {

        send(
            VertoEvent.Bye,
            VertoByeParamsOutgoing(
                cause= reason,
                causeCode= reason.getCode(),
                dialogParams= VertoDialogParams(
                    callID = callId,
                    login = vertoWs.getLogin(),
                    destinationNumber = phoneNumber,
                    remoteCallerIdNumber = phoneNumber
                ),
                sessid= vertoWs.getLogin()
            )
        )
    }

    fun answer(sdp: String, callId: String, number: String){

        send(
            VertoEvent.Answer,
            VertoMessageAnswerParams(
                sdp= sdp,
                sessid = vertoWs.sessionId,
                dialogParams = VertoDialogParams(
                    callID = callId,
                    login = vertoWs.getLogin(),
                    destinationNumber = number,
                    remoteCallerIdNumber = number
                )
            )
        )
    }

    fun hold(callId: String){
        send(
            VertoEvent.Modify,
            VertoMessageModifyParams(
                sessid = vertoWs.sessionId,
                action = VertoState.Hold,
                dialogParams = VertoDialogParams(
                    callID= callId,
                    destinationNumber = "",
                    remoteCallerIdNumber = "",
                    login=vertoWs.getLogin()
                )
            )
        )
    }

    fun unhold(callId: String){
        send(
            VertoEvent.Modify,
            VertoMessageModifyParams(
                sessid = vertoWs.sessionId,
                action = VertoState.Unhold,
                dialogParams = VertoDialogParams(
                    callID= callId,
                    destinationNumber = "",
                    remoteCallerIdNumber = "",
                    login=vertoWs.getLogin()
                )
            )
        )
    }

    fun transfer(callId: String, destination: String){
        send(
            VertoEvent.Modify,
            VertoMessageModifyParams(
                sessid = vertoWs.sessionId,
                action = VertoState.Transfer,
                destination = destination,
                dialogParams = VertoDialogParams(
                    callID= callId,
                    destinationNumber = "",
                    remoteCallerIdNumber = "",
                    login=vertoWs.getLogin()
                )
            )
        )
    }

    fun sendDtmf(callId: String, character: String) {
        send(
            VertoEvent.Info,
            VertoMessageInfoParams(
                sessid = vertoWs.sessionId,
                dtmf = character.toString(),
                dialogParams = VertoDialogParams(
                    callID= callId,
                    destinationNumber = "",
                    remoteCallerIdNumber = "",
                    login=vertoWs.getLogin()
                )
            )
        )
    }

}


