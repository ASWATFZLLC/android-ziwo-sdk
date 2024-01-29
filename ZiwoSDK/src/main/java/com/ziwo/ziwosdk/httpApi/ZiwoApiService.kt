package com.ziwo.ziwosdk.httpApi

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ZiwoApiService {
    @GET("monitor/ping")
    suspend fun checkCallCenter(@Query("callCenter") callCenter: String): Response<Void>

    @PUT("auth/device")
    suspend fun registerToken(@Query("deviceToken") deviceToken: String): Response<Void>

    @POST("auth/login")
    @FormUrlEncoded
    suspend fun login(
        @Query("callCenter") callCenter: String,
        @Field("username") userName: String,
        @Field("password") userPassword: String,
        @Field("vertoSessionId") vertoSessionId: String
    ): Response<ZiwoApiLoginData>
    @PUT("agents/status")
    @FormUrlEncoded
    suspend fun updateAgentStatus(@Field("number") status: Int): Response<Void>

    @PUT("agents")
    suspend fun updateAgent(@Body params: PutAgentParams): Response<Void>

    @GET("fs/webrtc/config")
    suspend fun getProfile(): Response<Void>

    @GET("agents/channels/calls/listQueues")
    suspend fun getListQueues(): Response<ListQueuesResponse>

    @PUT("agents/autologin")
    suspend fun autoLogin(@Body requestBody: RequestBody): Response<Void>

    @POST("auth/logout")
    suspend fun logout(): Response<String>

    @PUT("agents/autologout")
    suspend fun autoLogout(@Body requestBody: RequestBody): Response<String>

    @POST("users/password/reset")
    @FormUrlEncoded
    suspend fun resetPassword(@Field("username") userName: String): Response<Void>

    @GET("agent/agents")
    suspend fun getAgents(@Header("X-Use-Cache") useCache: Boolean,
        @Query("skip") skip: Int = 0): Response<ZiwoApiGetAgentsResponse>

    @GET("agent/crm/customers")
    suspend fun getCustomers(@Header("X-Use-Cache") useCache: Boolean,
                          @Query("skip") skip: Int = 0, @Query("limit") limit: Int = 0): Response<ZiwoApiGetCustomerResponse>

    @GET("static/countries")
    suspend fun getCountries(): Response<ZiwoApiCountriesResponse>
    @GET("agents/channels/calls/")
    suspend fun getAgentCalls(
        @Query("skip") skip: Int? = 0,
        @Query("directions[]") direction: String? = null,
        @Query("abandoned") abandoned: Boolean? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null,
        @Query("limit") limit: Int? = null

    ): Response<AgentCall>

    @GET("static/callRecordingsEndpoint")
    suspend fun getRecordingUrl(@Query("callId") callId: String): Response<RecordingUrlResponse>

    @GET("static/storageEndpoint")
    suspend fun getStorageEndpoint(): Response<RecordingUrlResponse>

}