package com.ziwo.ziwosdk

import com.ziwo.ziwosdk.httpApi.ZiwoApi
import com.ziwo.ziwosdk.verto.RTCPeerConnectionFactory
import com.ziwo.ziwosdk.verto.VertoByeReason
import com.ziwo.ziwosdk.utils.ziwoSdk.verto.VertoCommandsSender
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import java.time.LocalDateTime


class Call
    (
    val callId: String,
    private val verto: VertoCommandsSender,
    val phoneNumber: String,
    val login: String,
    val direction: ZiwoApi.Enums.CallDirection,
    private val initialEvent: ZiwoEventType,
    val initialPayload : Map<String, Any>? = null
    )
{

    val TAG = "[ZiwoSdk Call] $callId - "
    val eventsArray = mutableListOf<ZiwoCallEvent>()
    var rtcPeerConnection: PeerConnection? = null
    var io : MediaStream? = null
    var dtmfSequence = ""

    // control
    var isMute = false
        set(value) {
            if (io != null){
                for (track in io?.audioTracks!!) {
                    track.setEnabled(!value)
                }
            }
            field = value
        }

    init {
        pushState(initialEvent)
    }

    fun pushState( eventType: ZiwoEventType){

        val event = ZiwoCallEvent(event = eventType)
        eventsArray.add(event)
        verto.vertoWs.vertoHandler?.callStatusChanged(
            ZiwoCallChanged(
                call = this,
                event = event
            )
        )
    }

    fun hangup() {
        verto.hangup(callId, phoneNumber, VertoByeReason.NORMAL_CLEARING)
        destroy()
    }

    fun answer(){

        if( eventsArray.last().event !== ZiwoEventType.Ringing){
            return
        }

        pushState(ZiwoEventType.Answering)
        RTCPeerConnectionFactory(
            verto.vertoWs.context
        ).inbound(
            initialPayload?.get("sdp") as String ?: "",
            this,
        ){ pc, localStream ->
            rtcPeerConnection = pc
            io = localStream
            this.rtcPeerConnection?.localDescription?.description?.let { verto.answer(it, this.callId, this.phoneNumber) }
        }
    }

    fun hold(){
        verto.hold(this.callId)
    }

    fun unhold() {
        verto.unhold(this.callId)
    }

    fun transfer(destination: String) {
        verto.transfer(this.callId, destination)
    }

    fun sendDtmf(character: String): String {
        dtmfSequence += character
        verto.sendDtmf(this.callId, character)
        return dtmfSequence
    }

    fun destroy(){

        rtcClose()
        verto.vertoWs.callsList.remove(callId)
        pushState(ZiwoEventType.Destroy)
        verto.vertoWs.vertoHandler?.callEnded()
    }

    fun rtcClose(){
        rtcPeerConnection?.close()
        rtcPeerConnection = null
        pushState(ZiwoEventType.Disconnected)
    }


    companion object {

        enum class ZiwoEventType(value: String) {
            Error("error"),
            Connected("connected"),
            Disconnected("disconnected"),
            Requesting("requesting"),
            Trying("trying"),
            Early("early"),
            Ringing("ringing"),
            Answering("answering"),
            Active("active"),
            Held("held"),
            Hangup("hangup"),
            Destroy("destroy"),
            Recovering("recovering"),
        }

        data class ZiwoCallEvent(
            val time: LocalDateTime =  LocalDateTime.now(),
            val unixTimeStamp: Long =  System.currentTimeMillis() / 1000L,
            val event: ZiwoEventType
        )

        data class ZiwoCallChanged(
            val call: Call,
            val event: ZiwoCallEvent
        )

    }


}