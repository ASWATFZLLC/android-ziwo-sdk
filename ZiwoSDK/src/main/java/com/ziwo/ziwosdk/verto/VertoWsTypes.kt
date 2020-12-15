package com.ziwo.ziwosdk.verto

import com.google.gson.annotations.SerializedName
import com.ziwo.ziwosdk.Call
import kotlinx.android.parcel.RawValue
import java.util.*

interface VertoHandlerInterface {

    fun onVertoSocketStatusChange (status: WebSocketStatus)
    fun callStarted(call: Call)
    fun callEnded(call: Call)
    fun callStatusChanged(ziwoCallChanged: Call.Companion.ZiwoCallChanged)

}

enum class WebSocketStatus {
    Disconnected,
    PendingLogin,
    Ready,
    Failed,
    Closed,
    Retrying,
}

enum class VertoEvent {

    /** Successful login, Woho! */
    @SerializedName("verto.clientReady") ClientReady,
    /**  Call creation succeed */
    @SerializedName("verto.media") Media,
    /** Call from outside received */
    @SerializedName("verto.invite") Invite,
    /** Display call */
    @SerializedName("verto.display") Display,
    /** Verto Bye */
    @SerializedName("verto.bye") Bye,
    /** used internally to perform login */
    @SerializedName("login") LOGIN,

    // TODO
    @SerializedName("verto.attach")
    Attach,
    @SerializedName("verto.answer")
    Answer,
    @SerializedName("verto.info")
    Info,
    @SerializedName("verto.modify")
    Modify,
    @SerializedName("verto.pickup")
    Pickup,
}

data class ManualVertoMessage (
    val content: String,
)

enum class VertoByeReason() {
    NORMAL_CLEARING,
    CALL_REJECTED,
    ORIGINATOR_CANCEL;

    fun getCode(): VertoByeReasonCode {
        return when(this){
            NORMAL_CLEARING -> VertoByeReasonCode.NORMAL_CLEARING
            CALL_REJECTED -> VertoByeReasonCode.CALL_REJECTED
            ORIGINATOR_CANCEL -> VertoByeReasonCode.ORIGINATOR_CANCEL
        }
    }
}

enum class VertoByeReasonCode() {
    @SerializedName("16") NORMAL_CLEARING,
    @SerializedName("21") CALL_REJECTED,
    @SerializedName("487") ORIGINATOR_CANCEL,
}


open class VertoMessage<paramsType>(
    val id: Int? = null,
    val jsonrpc: String? = "2.0",
    val method: VertoEvent? = null,
    val params: paramsType? = null,
    val result: VertoResult? = null
    )


/**
 * A call specific properties
 */


enum class VertoNotificationMessage {
    @SerializedName("CALL CREATED") CallCreated,
    @SerializedName("CALL ENDED") CallEnded,
}

enum class VertoNotificationAction {
    @SerializedName("hold") Hold,
    @SerializedName("unhold") Unhold,
}

enum class VertoNotificationHoldState {
    @SerializedName("held") Held,
    @SerializedName("active") Active,
}

data class VertoResult(
    val callID: String,
    val cause: VertoByeReason?,
    val causeCode: VertoByeReasonCode?,
    val message: VertoNotificationMessage?,
    val sessid: String,
    val action: VertoNotificationAction?,
    val holdState: VertoNotificationHoldState?
)

/**
 * Verto Paremeters section
 */


enum class VertoState {
    @SerializedName("hold")  Hold,
    @SerializedName("unhold")  Unhold,
    @SerializedName("transfer")  Transfer,
}


data class VertoMessageInfoParams(
    val sessid: String,
    val dtmf: String,
    val dialogParams: VertoDialogParams
)

data class VertoMessageModifyParams(
    val action: VertoState,
    val sessid: String,
    val destination: String? = null,
    val dialogParams: VertoDialogParams
)

data class VertoMessageAnswerParams(
    val callID: String? = null, // Only used when outgoing message to signal other party answered
    val sdp: String,
    val sessid: String,
    val dialogParams: VertoDialogParams
)
data class VertoMessageLoginParams(
    val login: String? = null,
    val passwd: String? = null,
    val sessid: String? = null,
)

data class VertoMessageInviteParams(
    val callID: String,
    val calleeIdName: String,
    val calleeIdNumber: String,
    @SerializedName("caller_id_name")  val callerIdName: String = "",
    @SerializedName("caller_id_number")  val callerIdNumber: String = "",
    val displayDirection: String,
    val sdp: String,
    val vertoHPrimaryCallID: String
)

data class VertoMessageAttachParams(
    val callID: String,
    @SerializedName("callee_id_name") val calleeIdName: String,
    @SerializedName("callee_id_number") val calleeIdNumber: String,
    @SerializedName("caller_id_name") val callerIdName: String,
    @SerializedName("caller_id_number") val callerIdNumber: String,
    @SerializedName("display_direction") val displayDirection: String,
    val sdp: String
)

data class VertoMessageAttachParamsSending(
    val sessid: String? = null,
    val sdp: String,
    val dialogParams: VertoDialogParams
)

data class VertoInviteParams(
    val sessid: String? = null,
    val sdp: String,
    val dialogParams: VertoDialogParams
)

data class VertoMediaParams(
    val callID: String,
    val sdp: String
)

data class VertoDisplayParams(
    @SerializedName("callID")  val callID: String,
    @SerializedName("callee_id_name")  val calleeIdName: String,
    @SerializedName("callee_id_number")  val calleeIdNumber: String,
    @SerializedName("caller_id_name")  val callerIdName: String,
    @SerializedName("caller_id_number")  val callerIdNumber: String,
    @SerializedName("display_direction")  val displayDirection: String,
    @SerializedName("display_name")  val displayName: String,
    @SerializedName("display_number")  val displayNumber: String
)

data class VertoByeParamsOutgoing(
    val cause: VertoByeReason,
    val causeCode: VertoByeReasonCode,
    val dialogParams: VertoDialogParams,
    val sessid: String
)

data class VertoByeParamsIncoming(
    val cause: VertoByeReason,
    val causeCode: VertoByeReasonCode,
    val callID: String
)

data class VertoDialogParams(
    val callID: String,
    @SerializedName("caller_id_name")  val callerIdName: String = "",
    @SerializedName("caller_id_number")  val callerIdNumber: String = "",
    val dedEnc: Boolean = false,
    @SerializedName("destination_number") val destinationNumber: String,
    val incomingBandwidth: String = "default",
    val localTag: @RawValue Any? = null,
    val login: String,
    val outgoingBandwidth: String = "default",
    @SerializedName("remote_caller_id_name") val caller_id_name: String? = "Outbound Call",
    @SerializedName("remote_caller_id_number") val remoteCallerIdNumber: String,
    val screenShare: Boolean = false,
    val tag: String = UUID.randomUUID().toString(),
    val useCamera: Boolean = false,
    val useMic: Boolean = true,
    val useSpeak: Boolean = true,
    val useStereo: Boolean = true,
    val videoParams: VideoParams? = null,
    val audioParams: AudioParams? = AudioParams()
    )

data class AudioParams(
    val googAutoGainControl: Boolean = false,
    val googHighpassFilter: Boolean = false,
    val googNoiseSuppression: Boolean = false
)

class VideoParams
