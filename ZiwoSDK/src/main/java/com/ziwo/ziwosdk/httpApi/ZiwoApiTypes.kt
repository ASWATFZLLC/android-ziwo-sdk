package com.ziwo.ziwosdk.httpApi

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue



enum class AgentStatus(val code: Int){
    @SerializedName("Retrying") Retrying(-1),
    @SerializedName("Logged Out") LoggedOut(0),
    @SerializedName("Available") Available(1),
    @SerializedName("On Break") OnBreak(2),
    @SerializedName("Meeting") Meeting(3),
    @SerializedName("Outgoing") Outgoing(4),
}





/** ------------- Return Params -------------- */

/**
 *  `/auth/login`
 */
@Parcelize
data class ZiwoApiLoginData(
    val content: ZiwoApiLoginContentData,
    val result: Boolean
)  : Parcelable

@Parcelize
data class ZiwoApiLoginContentData(
    val access_token: String,
    val autoAnswer: Boolean,
    val ccLogin: String,
    val ccPassword: String,
    val contactNumber: @RawValue  Any,
    val createdAt: String,
    val firstName: String,
    val id: Int,
    val lastLoginAt: String,
    val lastName: String,
    val noAnswerDelayTime: @RawValue  Any,
    val noAnswerTimeout: Int,
    val outboundRoaming: Boolean,
    val photo: String?,
    val profileType: String,
    val roamingContactNumber: String,
    val roamingTimeout: Int,
    val roleId: Int,
    val status: String,
    val type: String,
    val updatedAt: String,
    val username: String,
    val wrapUpTime: Int,
    val languageCode : String?,
    val countryCode: String?,
)  : Parcelable


/**
 * endpoint `/agent/agents`
 * List agents
 */

@Parcelize
data class ZiwoApiGetAgents(
    val content: List<ZiwoApiGetAgentsContent>,
    val result: Boolean
)  : Parcelable

@Parcelize
data class ZiwoApiGetAgentsContent(
    val ccLogin: String,
    val firstName: String,
    val id: Int,
    val lastName: String,
    val liveInfo: ZiwoApiGetAgentsLiveInfo?,
    val photo: String?
)  : Parcelable

@Parcelize
data class ZiwoApiGetAgentsLiveInfo(
    val state: String,
    val status: String
)  : Parcelable

/**
* endpoint `/static/languages`
* List supported countries
*/
@Parcelize
data class ZiwoApiCountries(
    val content: List<ZiwoApiCountriesContent>,
    val result: Boolean
)  : Parcelable

@Parcelize
data class ZiwoApiCountriesContent(
    val callingCode: String?,
    val countryCode: String,
    val currency: String,
    val name: String,
    val officialName: String,
    val region: String,
    val sample: ZiwoApiCountriesContentSample,
    val tld: String
)  : Parcelable

@Parcelize
data class ZiwoApiCountriesContentSample(
    val numbers: ZiwoApiCountriesContentNumbers
)  : Parcelable

@Parcelize
data class ZiwoApiCountriesContentNumbers(
    val mobile: ZiwoApiCountriesContentMobile
)  : Parcelable

@Parcelize
data class ZiwoApiCountriesContentMobile(
    val internationalFormat: String,
    val nationalFormat: String
)  : Parcelable


/**
 * endpoint `/agents/channels/calls/`
 */
@Parcelize
data class AgentCall(
    val content: List<AgentCallContent>,
    val info: AgentCallInfo,
    val result: Boolean
)  : Parcelable

@Parcelize
data class AgentCallContent (
    val agentCCLogin: String,
    val agentId: Int,
    val agentRingStartDateTime: String,
    val agentRingStartedAt: String,
    val agentWaitTime: Int,
    val answeredAt: String,
    val answeredDateTime: String?,
    val audioQuality: Int,
    val callID: String,
    val callerIDName: String,
    val callerIDNumber: String?,
    val channelName: String,
    val createdAt: String,
    val didCalled: String,
    val direction: String,
    val disposition: String,
    val duration: Int,
    val endDateTime: String,
    val endedAt: String,
    val extendedInfo: AgentCallContentExtendedInfo,
    val flags: Int,
    val gateway: String,
    val hangupBy: String,
    val hangupCause: String,
    val holdTime: @RawValue Any,
    val id: Int,
    val ivrFile: String,
    val ivrTime: @RawValue Any,
    val lostInIVR: Boolean,
    val nonFCRRepeats: Int,
    val nonWorkingHours: Boolean,
    val numberId: Int,
    val otherSideUserId: @RawValue  Any,
    val position: String,
    val positionId: Int,
    val queueAnsweredAt: String,
    val queueAnsweredDateTime: String,
    val queueEnterDateTime: String,
    val queueEnteredAt: String,
    val queueId: Int,
    val queueName: String?,
    val queueTalkTime: Int,
    val queueWaitTime: Int,
    val recordingFile: String?,
    val result: String,
    val ringTime: Int,
    val startDateTime: String,
    val startedAt: String,
    val status: String,
    val talkTime: Int,
    val updatedAt: String,
    val voicemail: Boolean,
    val emotions: AgentCallContentEmotion?
)  : Parcelable

@Parcelize
data class AgentCallContentEmotion(
    val chunks: List<AgentCallContentEmotionChunk?>,
    val full: AgentCallContentEmotionFull
)  : Parcelable

@Parcelize
data class AgentCallContentEmotionChunk(
    val anger: Double?,
    val error: String?,
    val fear: Double?,
    val happiness: Double?,
    val neutrality: Double?,
    val sadness: Double?
)  : Parcelable

@Parcelize
data class AgentCallContentEmotionFull(
    val anger: Double,
    val fear: Double,
    val happiness: Double,
    val neutrality: Double,
    val sadness: Double
)  : Parcelable

@Parcelize
data class AgentCallInfo(
    val totalRecords: Int
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfo(
    val agents: AgentCallContentExtendedInfoAgents,
    val numbers: AgentCallContentExtendedInfoNumbers,
    val positions: AgentCallContentExtendedInfoPositions,
    val queues: AgentCallContentExtendedInfoQueues
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoAgents(
    val autoAnswer: Boolean,
    val ccLogin: String,
    val ccPassword: String,
    val contactNumber: @RawValue  Any,
    val firstName: String,
    val id: Int,
    val lastLoginAt: String,
    val lastName: String,
    val liveInfo: AgentCallContentExtendedInfoAgentsLiveInfo,
    val noAnswerDelayTime: @RawValue  Any,
    val noAnswerTimeout: Int,
    val position: AgentCallContentExtendedInfoAgentsPosition,
    val queues: List<AgentCallContentExtendedInfoAgentsQueue>,
    val roamingContactNumber: @RawValue  Any,
    val roamingTimeout: Int,
    val roleId: Int,
    val type: String,
    val username: String,
    val wrapUpTime: Int,
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoNumbers(
    val beyondTimeslotsLinkData: String,
    val beyondTimeslotsLinkType: String,
    val createdAt: String,
    val deletedAt: @RawValue  Any,
    val did: String,
    val didCalled: String,
    val didDisplay: String,
    val id: Int,
    val linkData: String,
    val linkType: String,
    val status: String,
    val timeslots: List<@RawValue  Any>,
    val updatedAt: String
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoPositions(
    val agent: AgentCallContentExtendedInfoPositionsAgent,
    val connected: Boolean,
    val id: Int,
    val liveInfo: AgentCallContentExtendedInfoPositionsLiveInfo,
    val name: String,
    val type: String
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoQueues(
    val agents: List<AgentCallContentExtendedInfoQueuesAgent>,
    val announcementType: String,
    val callerIDNumber: String,
    val id: Int,
    val liveCalls: List<@RawValue  Any>,
    val maxWaitTime: Int,
    val moh: String,
    val name: String,
    val priority: Int,
    val status: String,
    val strategyType: String
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoAgentsLiveInfo(
    val callsAnswered: String,
    val contactString: String,
    val lastBridgeEPOCH: String,
    val lastStatusChangeEPOCH: String,
    val lastUnBridgeEPOCH: String,
    val noAnswerCount: String,
    val position: String,
    val state: String,
    val status: String,
    val talkTime: String,
    val wrapUpTime: String
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoAgentsPosition(
    val connected: Boolean,
    val id: Int,
    val name: String,
    val type: String
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoAgentsQueue(
    val announcementType: String,
    val callerIDNumber: @RawValue  Any,
    val id: Int,
    val maxWaitTime: Int,
    val moh: String,
    val name: String,
    val priority: Int,
    val skill: AgentCallContentExtendedInfoAgentsQueueSkill,
    val strategyType: String,
    val waitTimeoutDID: String
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoAgentsQueueSkill(
    val priority: Int
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoPositionsAgent(
    val autoAnswer: Boolean,
    val ccLogin: String,
    val ccPassword: String,
    val contactNumber: @RawValue  Any,
    val firstName: String,
    val id: Int,
    val lastLoginAt: String,
    val lastName: String,
    val noAnswerDelayTime: @RawValue  Any,
    val noAnswerTimeout: Int,
    val roamingContactNumber: @RawValue  Any,
    val roamingTimeout: Int,
    val roleId: Int,
    val type: String,
    val username: String,
    val wrapUpTime: Int
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoPositionsLiveInfo(
    val device: String,
    val networkIP: String,
    val pingStatus: String,
    val pingTime: String,
    val position: String
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoQueuesAgent(
    val autoAnswer: Boolean,
    val ccLogin: String,
    val ccPassword: String,
    val contactNumber: @RawValue  Any,
    val createdAt: String,
    val firstName: String,
    val id: Int,
    val lastLoginAt: String,
    val lastName: String,
    val noAnswerDelayTime: @RawValue  Any,
    val noAnswerTimeout: Int,
    val photo: String?,
    val roamingContactNumber: @RawValue  Any,
    val roamingTimeout: Int,
    val roleId: Int,
    val skill: AgentCallContentExtendedInfoQueuesAgentSkill,
    val status: String,
    val type: String,
    val updatedAt: String,
    val username: String,
    val wrapUpTime: Int
)  : Parcelable

@Parcelize
data class AgentCallContentExtendedInfoQueuesAgentSkill(
    val priority: Int
)  : Parcelable

/**
 * endpoint `/static/callRecordingsEndpoint`
 */
@Parcelize
data class CallRecordingEndpoint(
    val content: CallRecordingEndpointContent,
    val info: CallRecordingEndpointInfo,
    val result: Boolean
)  : Parcelable

@Parcelize
data class CallRecordingEndpointContent(
    val endpoint: String
)  : Parcelable

@Parcelize
class CallRecordingEndpointInfo(
)  : Parcelable

/**
 * endpoint `/static/callRecordingsEndpoint`
 */
@Parcelize
data class CallStorageEndpoint(
    val content: CallStorageEndpointContent,
    val info: CallStorageEndpointInfo,
    val result: Boolean
)  : Parcelable
@Parcelize
data class CallStorageEndpointContent(
    val endpoint: String
)  : Parcelable

@Parcelize
class CallStorageEndpointInfo(
)  : Parcelable

/**
 *  endpoint `/agents/channels/calls/listQueues`
 */
data class ListQueues(
    val content: List<ListQueuesContent>,
    val info: ListQueuesInfo,
    val result: Boolean
)

data class ListQueuesContent(
    val announcementType: Any,
    val callerIDNumber: String,
    val createdAt: String,
    val deletedAt: Any,
    val enableCookies: Boolean,
    val extension: String?,
    val id: Int,
    val image: String?,
    val language: String,
    val maxWaitTime: Int,
    val moh: String,
    val name: String,
    val nonWorkingHoursDID: String,
    val priority: Int,
    val status: String,
    val strategyType: String,
    val surveyRequired: Boolean,
    val timeslots: String,
    val updatedAt: String,
    val urgentMessage: Any,
    val waitTimeoutDID: String
)

class ListQueuesInfo()


/** ------------- Function Params -------------- */

data class PutAgentParams(
    val firstName: String? = null,
    val lastName: String? = null,
    val photo: String? = null,
    val password: String? = null,
    val currentPassword: String? = null,
    val countryCode: String? = null,
    val languageCode: String? = null,
)