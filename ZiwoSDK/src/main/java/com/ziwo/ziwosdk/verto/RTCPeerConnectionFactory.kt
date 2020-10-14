package com.ziwo.ziwosdk.verto

import android.content.Context
import android.util.Log
import com.ziwo.agent.utils.ziwoSdk.verto.VertoCommandsSender
import com.ziwo.ziwosdk.Call
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.webrtc.*


class RTCPeerConnectionFactory  constructor(
    private val context: Context
) {

    val TAG = "RTCPeerConnectionFactory"
    val stunServer =listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        //Initialize PeerConnectionFactory globals.
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(initializationOptions)
        PeerConnectionFactory.builder().createPeerConnectionFactory()

    }

    /** create rtc client trying to connect to outside
     * 1. create localDescription After ice gathering
     * 2. send localDescription to ziwo
     * */
    fun outbound(
        vertoCommandsSender: VertoCommandsSender,
        call: Call,
        ): RtcCollection {

        var pc: PeerConnection? = null
        // pc observer
        val pcObserver: PeerConnection.Observer =
            object: MyPeerConnectionObserver("outbound_createPC",  call) {
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    super.onIceGatheringChange(state)
                    // TODO: dont send if call is has already sent verto.bye , please add event closing
                    println("oooo ${call.eventsArray.last().event}")
                    if (
                        state == PeerConnection.IceGatheringState.COMPLETE &&
                        call.eventsArray.last().event != Call.Companion.ZiwoEventType.Error
                    ) {
                        println("cccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                        vertoCommandsSender.send(
                            VertoEvent.Invite,
                            VertoInviteParams(
                                sdp = pc!!.localDescription.description,
                                sessid = vertoCommandsSender.vertoWs.sessionId,
                                dialogParams = VertoDialogParams(
                                    callID = call.callId,
                                    login = call.login,
                                    destinationNumber = call.phoneNumber,
                                    remoteCallerIdNumber = call.phoneNumber
                                    )
                            )
                        )
                    }
                }
            }

        pc = peerConnectionFactory.createPeerConnection(
            stunServer,
            pcObserver
        )!!

        // create offer
        val sdpObserver = object : MySdpObserver("OUTBOUND_sdpObserver", call){
            override fun onCreateSuccess(sdp: SessionDescription?) {
                super.onCreateSuccess(sdp)
                pc.setLocalDescription( MySdpObserver("setLocalDescription", call) , sdp)
            }
        }

        // add stream
        val audioConstraints = MediaConstraints()
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        val audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        val localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("myAndroidAudio")
        localStream.addTrack(localAudioTrack)
        pc.addStream(localStream)

        // create the offer
        pc.createOffer(sdpObserver, audioConstraints)


        return RtcCollection(
            pc=pc,
            localStream=localStream
        )
    }

    /** create rtc client receiving a call
     * 1. set remote sdp
     * 2. send localDescription to ziwo
     * */
    fun inbound(
        sdp: String,
        call: Call,
        onComplete: (pc : PeerConnection, localStream: MediaStream) -> Unit
    ) {


        var pc: PeerConnection? = null

        // create audio constrains
        val audioConstraints = MediaConstraints()
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        val audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        val localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("myAndroidAudio")
        localStream.addTrack(localAudioTrack)

        // setRemote
        val setRemoteObserver = object : MySdpObserver("INBOUND_setRemote", call){
            override fun onSetSuccess() {
                super.onSetSuccess()
                pc!!.createAnswer(  object : MySdpObserver("INBOUND_createAnswer", call){
                    override fun onCreateSuccess(sdp: SessionDescription?) {
                        super.onCreateSuccess(sdp)
                        pc!!.setLocalDescription( object: MySdpObserver("setLocalDescription", call){
                            override fun onSetSuccess() {
                                super.onSetSuccess()

                            }
                        }, sdp)
                    }
                },audioConstraints)
            }
        }

        // pc observer
        val pcObserver: PeerConnection.Observer =
            object : MyPeerConnectionObserver("inbound_createPeerConnection", call) {
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    super.onIceGatheringChange(state)
                    if (
                        state == PeerConnection.IceGatheringState.COMPLETE &&
                        call.eventsArray.last().event != Call.Companion.ZiwoEventType.Error
                    ) {
                        onComplete(pc!!, localStream)
                    }
                }
            }

        pc = peerConnectionFactory.createPeerConnection(
            stunServer,
            pcObserver
        )!!

        // add stream
        pc.addStream(localStream)

        // create the answer
        pc.setRemoteDescription(
            setRemoteObserver,
            SessionDescription(SessionDescription.Type.OFFER, sdp)
        )

    }

    /** create rtc client receiving a call
     * 1. set remote sdp
     * 2. send localDescription to ziwo
     * */
    fun recover(
        vertoCommandsSender: VertoCommandsSender,
        sdp: String,
        call: Call,
    ): RtcCollection {


        var pc: PeerConnection? = null

        // create audio constrains
        val audioConstraints = MediaConstraints()
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        val audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        val localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("myAndroidAudio")
        localStream.addTrack(localAudioTrack)

        // pc observer
        val pcObserver  =
            object : MyPeerConnectionObserver("Recovering_createPeerConnection", call) {

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    super.onIceGatheringChange(state)
                    if (state == PeerConnection.IceGatheringState.COMPLETE){
                        println(" im sending correctly now     0000000000000000")
                        println( "pc.localDescription.description ${pc!!.localDescription.description.length}")
                        vertoCommandsSender.send(
                            VertoEvent.Attach,
                            VertoMessageAttachParamsSending(
                                sdp = pc!!.localDescription.description,
                                sessid = vertoCommandsSender.vertoWs.sessionId,
                                dialogParams = VertoDialogParams(
                                    callID = call.callId,
                                    login = call.login,
                                    destinationNumber = call.phoneNumber,
                                    remoteCallerIdNumber = call.phoneNumber,

                                    )
                            )
                        )

                        // find out last event before disconnect
                        var lastEvent: Call.Companion.ZiwoEventType? = null
                        for ( eventLog in call.eventsArray.asReversed()){
                            if (
                                eventLog.event !== Call.Companion.ZiwoEventType.Disconnected
                                && eventLog.event !== Call.Companion.ZiwoEventType.Recovering
                            ){
                                lastEvent = eventLog.event
                                break
                            }
                        }
                        println("------------------ $lastEvent")

                        // push or close last event
                        if (
                            lastEvent == Call.Companion.ZiwoEventType.Active
                            || lastEvent == Call.Companion.ZiwoEventType.Held
                        ) {
                            call.pushState(lastEvent)

                        } else {
                            // this Globalscope is needed because of weird bug dont remove it other rtc.close() will get stuck
                            GlobalScope.launch {
                                call.hangup()
                            }
                        }


                    }
                }

            }


        // setRemote
        val setRemoteObserver = object : MySdpObserver("Recovering_setRemote", call){
            override fun onSetSuccess() {
                super.onSetSuccess()
                pc!!.createAnswer(  object : MySdpObserver("Recovering_createAnswer", call){
                    override fun onCreateSuccess(sdp: SessionDescription?) {
                        super.onCreateSuccess(sdp)
                        pc!!.createAnswer(  object : MySdpObserver("Recovering_createAnswer", call){
                            override fun onCreateSuccess(sdp: SessionDescription?) {
                                super.onCreateSuccess(sdp)
                                pc!!.setLocalDescription( object : MySdpObserver("Recovering_setLocalDescription", call){ }, sdp)
                            }
                        },audioConstraints)
                    }
                },audioConstraints)
            }
        }



        pc = peerConnectionFactory.createPeerConnection(
            stunServer,
            pcObserver
        )!!

        // add stream
        pc.addStream(localStream)

        // create the answer
        pc.setRemoteDescription(
            setRemoteObserver,
            SessionDescription(SessionDescription.Type.OFFER, sdp)
        )


        return RtcCollection(
            pc=pc,
            localStream=localStream
        )
    }

    data class RtcCollection(
        val pc : PeerConnection,
        val localStream: MediaStream
    )

}


/**
 * helper observer1
 */
open class MySdpObserver(private val TAG: String, private val call: Call) :SdpObserver{
    override fun onSetFailure(p0: String?) {
        Log.d(TAG, "onSetFailure $p0")
        call.pushState(Call.Companion.ZiwoEventType.Error)
    }

    override fun onSetSuccess() {
        Log.d(TAG, "onSetSuccess ")
    }

    override fun onCreateSuccess(p0: SessionDescription?) {
        Log.d(TAG, "onCreateSuccess $p0")
    }

    override fun onCreateFailure(p0: String?) {
        Log.d(TAG, "onCreateFailure $p0")
        call.pushState(Call.Companion.ZiwoEventType.Error)
//        call.destroy() // we close and follow verto recovery protocol
    }

}


/**
 * helper observer2
 */
open class MyPeerConnectionObserver(private val TAG: String, private val call: Call) : PeerConnection.Observer {

    public var localDescriptionHasBeenSet = false

    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        Log.d(TAG,"onIceCandidate $iceCandidate")
    }

    override fun onDataChannel(p0: DataChannel?) {
        Log.d(TAG, "onDataChannel $p0")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Log.d(TAG, "onIceConnectionReceivingChange $p0")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        Log.d(TAG, "onIceConnectionChange $p0")
//        if ( p0 == PeerConnection.IceConnectionState.FAILED){
//            call.pushState(Call.Companion.ZiwoEventType.Error)
//            call.destroy() // we close and follow verto recovery protocol
//        }
    }

    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
        Log.d(TAG, "onIceGatheringChange $state")
    }

    override fun onAddStream(p0: MediaStream?) {
        Log.d(TAG, "onAddStream $p0")
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        Log.d(TAG, "onSignalingChange $p0")
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        Log.d(TAG, "onIceCandidatesRemoved $p0")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        Log.d(TAG, "onRemoveStream $p0")
    }

    override fun onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded $")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Log.d(TAG, "onAddTrack $p0 $p1")
    }

}