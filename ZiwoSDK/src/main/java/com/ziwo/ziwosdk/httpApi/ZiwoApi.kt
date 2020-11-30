package com.ziwo.ziwosdk.httpApi

import com.google.gson.Gson
import com.ziwo.ziwosdk.Ziwo
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException


class ZiwoApi(private val ziwo: Ziwo) {

    // client basics
    private val TAG = "[ZiwoApi]"
    private val client = OkHttpClient()
    private val gson = Gson()


    // login state
    private var accessToken = ""
    private var baseUrl = "" // api url

    // Custom Exception
    open class ZiwoException(message:String): Exception(message)
    class UserIsAdminException(message:String): ZiwoException(message)

    /**
     * useful for when restoring session manually offline
     */
    fun setCredentials(callCenter: String? = null, accessToken: String? = null){

        if (callCenter != null) {
            baseUrl  = "https://$callCenter-api.aswat.co"
        }
        if (accessToken != null) {
            this.accessToken = accessToken
        }
    }

    /**
     * Checks if the ziwo instance exist and saves the api url
     * Adds a [baseUrl] to this instance.
     */
    fun checkCallCenter(callCenter: String): Boolean {

        return try {
            val request = Request.Builder()
                .url("https://$callCenter-api.aswat.co/monitor/ping")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            true
        } catch (ex: Exception){
            false
        }

    }

    /**
     * endpoint `/auth/device`
     * Register the device's firebase token with ziwo
     */
    fun registerToken(deviceToken: String) {

        val formBody: RequestBody =
            FormBody.Builder()
                .add("deviceToken", deviceToken)
                .build()

        val request = Request.Builder()
            .url(baseUrl + "auth/device")
            .put(formBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        ziwo.logger(TAG, body)

    }

    /**
     * endpoint `/auth/login`
     * Adds a [authToken] to this instance.
     */
    fun login(callCenter: String, userName: String, userPassword: String): ZiwoApiLoginContentData {

        baseUrl  = "https://$callCenter-api.aswat.co"

        val formBody: RequestBody =
            FormBody.Builder()
                .add("username", userName)
                .add("password", userPassword)
                .add("remember", "true")
                .build()

        val request = Request.Builder()
            .url(baseUrl + "/auth/login")
            .post(formBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        ziwo.logger(TAG, body)

        val bodyParsed = gson.fromJson(body, ZiwoApiLoginData::class.java)
        if( bodyParsed.content.type != "agent"){
            throw UserIsAdminException("attempt login with non agent type user")
        }

        accessToken = bodyParsed.content.access_token

        return bodyParsed.content

    }

    /**
     * endpoint `/agents/autologin`
     * enables agent to make and receive calls
     */
    fun updateAgentStatus(status: AgentStatus) {

        val formBody: RequestBody =
            FormBody.Builder()
                .add("number", status.code.toString())
                .build()

        val request = Request.Builder()
            .url(baseUrl + "/agents/status")
            .addHeader("access_token", this.accessToken)
            .put(formBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        ziwo.logger(TAG, body)

    }


    /**
     * endpoint `/agents`
     * enables agent to make and receive calls
     */
    fun updateAgent(params: PutAgentParams) {

        val formBodyBuilder: FormBody.Builder = FormBody.Builder()

        params.countryCode?.let {
            formBodyBuilder.add("countryCode", it)
        }
        params.currentPassword?.let {
            formBodyBuilder.add("currentPassword", it)
        }
        params.firstName?.let {
            formBodyBuilder.add("firstName", it)
        }
        params.languageCode?.let {
            formBodyBuilder.add("languageCode", it)
        }
        params.password?.let {
            formBodyBuilder.add("password", it)
        }
        params.photo?.let {
            formBodyBuilder.add("photo", it)
        }
        params.lastName?.let {
            formBodyBuilder.add("lastName", it)
        }


        val request = Request.Builder()
            .url(baseUrl + "/agents")
            .addHeader("access_token", this.accessToken)
            .put(formBodyBuilder.build())
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

    }

    /**
     * endpoint `/agents/autologin`
     * enables agent to make and receive calls
     */
    fun getProfile() {

        val request = Request.Builder()
            .url(baseUrl + "/fs/webrtc/config")
            .addHeader("access_token", this.accessToken)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        ziwo.logger(TAG, body)

    }

    /**
     * endpoint `/agents/channels/calls/listQueues`
     * enables agent to make and receive calls
     */
    fun getListQueues(): List<ListQueuesContent> {

        val request = Request.Builder()
            .url(baseUrl + "/agents/channels/calls/listQueues")
            .addHeader("access_token", this.accessToken)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        val bodyParsed = gson.fromJson(body, ListQueues::class.java)

        ziwo.logger(TAG, body)

        return bodyParsed.content
    }

    /**
     * endpoint `/agents/autologin`
     * enables agent to make and receive calls
     */
    fun autoLogin() {

        val requestBody = "{}"
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(baseUrl + "/agents/autologin")
            .addHeader("access_token", this.accessToken)
            .put(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        ziwo.logger(TAG, body)

    }

    /**
     * endpoint `/agents/logout/`
     *  logout other open sessions
     */
    fun autoLogout(): String {



        val requestBody = "{}"
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(baseUrl + "/agents/logout?bc=${System.currentTimeMillis()}")
            .addHeader("access_token", this.accessToken)
            .put(requestBody)
            .build()

        val response = client.newCall(request).execute()

        val body = response.body!!.string()
        if ( response.code != 412){ // ignore if already logged off
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        }

       return body

    }

    /**
     * endpoint `/auth/login`
     * Adds a [authToken] to this instance.
     */
    fun restPassword(userName: String){

        val formBody: RequestBody =
            FormBody.Builder()
                .add("username", userName)
                .build()

        val request = Request.Builder()
            .url(baseUrl + "/users/password/reset")
            .post(formBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        ziwo.logger(TAG, body)
    }

    /**
     * endpoint `agent/agents`
     * List agents
     */
    fun getAgents(
        skip: Int = 0
        ): List<ZiwoApiGetAgentsContent> {

        val request = Request.Builder()
            .url(baseUrl + "/agent/agents?limit=50&skip=$skip&order=firstName")
            .addHeader("access_token", this.accessToken)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        ziwo.logger(TAG, body)
        val bodyParsed = gson.fromJson(body, ZiwoApiGetAgents::class.java)

        return bodyParsed.content
    }

    /**
     * endpoint `agent/agents`
     * List agents
     */
    fun getCountries(): List<ZiwoApiCountriesContent> {

        // TODO: add limits

        val request = Request.Builder()
            .url(baseUrl + "/static/countries")
            .addHeader("access_token", this.accessToken)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        val bodyParsed = gson.fromJson(body, ZiwoApiCountries::class.java)

        return bodyParsed.content
    }

    /**
     * endpoint `/agents/channels/calls/    `
     * returns endpoint to fetch call recording
     */
    fun getAgentCalls(
        skip: Int = 0,
        direction: CallDirection = CallDirection.ALL,
        fromDate: String? = null, //2016-02-04
        toDate: String? = null, //2016-02-04
    ): AgentCall {

        val url = "$baseUrl/agents/channels/calls/".toHttpUrlOrNull()!!.newBuilder()

        when(direction){
             CallDirection.INBOUND -> {
                 url.addQueryParameter("directions[]", direction.value)
                 url.addQueryParameter("abandoned", "false")
             }
             CallDirection.OUTBOUND -> {
                 url.addQueryParameter("directions[]", direction.value)
                 url.addQueryParameter("abandoned", "false")
             }
             CallDirection.MISSED -> {
                 url.addQueryParameter("abandoned", "true")
             }
            else ->{}
        }

        if (fromDate != null){
            url.addQueryParameter("fromDate", fromDate)
        } else {
            url.addQueryParameter("fromDate", "1990-01-01")
        }
        if (toDate != null){
            url.addQueryParameter("toDate", toDate)
        } else {
            url.addQueryParameter("toDate", "2050-01-01")
        }

        if ( skip > 0){
            url.addQueryParameter("skip", skip.toString())
        }
       url.addQueryParameter("limit", "20")

        val request = Request.Builder()
            .url(url.build())
            .addHeader("access_token", this.accessToken)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()

        return gson.fromJson(body, AgentCall::class.java)
    }

    fun getRecordingUrl( callId: String): String {
        val request = Request.Builder()
            .url(baseUrl + "/static/callRecordingsEndpoint")
            .addHeader("access_token", this.accessToken)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        val bodyParsed = gson.fromJson(body, CallRecordingEndpoint::class.java)

        // TODO: convert to endpoint instead of a function
        return "${bodyParsed.content.endpoint}$callId"
    }

    fun getStorageEndpoint(): String {
        val request = Request.Builder()
            .url(baseUrl + "/static/storageEndpoint")
            .addHeader("access_token", this.accessToken)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val body = response.body!!.string()
        val bodyParsed = gson.fromJson(body, CallStorageEndpoint::class.java)

        return "$baseUrl${bodyParsed.content.endpoint}"
    }

    companion object Enums {

        enum class CallDirection(var value: String) {
            INBOUND("inbound"),
            OUTBOUND("outbound"),
            MISSED("missed"),
            ALL("")
        }

    }


}

