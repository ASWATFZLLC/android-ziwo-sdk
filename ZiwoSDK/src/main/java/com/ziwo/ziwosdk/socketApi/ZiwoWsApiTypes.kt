package com.ziwo.ziwosdk.socketApi

import com.ziwo.ziwosdk.httpApi.AgentStatus
import com.ziwo.ziwosdk.verto.WebSocketStatus

interface ZiwoWsHandlerInterface {
    fun onApiSocketStatusChange (status: WebSocketStatus)
    fun onAgentStatusChange ( status: AgentStatus)
}


class WsApiRoutes {
    companion object{
        const val GetLiveStatus = "GET /live/status"
        const val GetProfile = "GET /profile"

    }
}


data class WsApiRes<t>(
    val content: t,
    val info: Info,
    val result: Boolean
)

data class GetLiveStatus(
    val force: Boolean,
    val since: Int,
    val status: AgentStatus?,
)

data class Info(
    val status: Int
)

