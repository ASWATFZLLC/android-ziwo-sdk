package com.ziwo.ziwosdk.httpApi

import android.util.Log
import com.google.gson.Gson
import com.ziwo.ziwosdk.Ziwo
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class ZiwoApi(private val ziwo: Ziwo) {

    // client basics
    private val TAG = "[ZiwoApi]"
    private val gson = Gson()

    // login state
    private var accessToken = ""
    private var baseUrl = "" // api url

    // Set up logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Set up OkHttpClient with interceptor
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(HeaderInterceptor(accessToken))
        .build()

    // Set up Retrofit with OkHttpClient and GsonConverterFactory
    private var retrofit: Retrofit = Retrofit.Builder()
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Instantiate service
    private var service: ZiwoApiService

    init {
        service = retrofit.create(ZiwoApiService::class.java)
    }

    // Custom Exception
    open class ZiwoException(message: String) : Exception(message)
    class UserIsAdminException(message: String) : ZiwoException(message)

    /**
     * useful for when restoring session manually offline
     */
    fun setCredentials(callCenter: String? = null, accessToken: String? = null) {
        if (callCenter != null) {
            baseUrl = "https://$callCenter-api.aswat.co"
            retrofit = retrofit.newBuilder().baseUrl(baseUrl).build() // Update base URL
            service = retrofit.create(ZiwoApiService::class.java) // Update service
        }
        if (accessToken != null) {
            this.accessToken = accessToken
        }
    }

    suspend fun checkCallCenter(callCenter: String): Boolean {
        return try {
            val response = service.checkCallCenter(callCenter)
            response.isSuccessful
        } catch (ex: Exception) {
            false
        }
    }

    suspend fun registerToken(deviceToken: String) {
        try {
            val response = service.registerToken(deviceToken)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: IOException) {
            // handle exception or rethrow it
        }
    }

    suspend fun login(callCenter: String, userName: String, userPassword: String, vertoSessionId: String): ZiwoApiLoginContentData? {
        try {
            val response = service.login(callCenter, userName, userPassword, vertoSessionId)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body()?.content
        } catch (ex: IOException) {
            // handle exception or rethrow it
            return null
        }
    }
    suspend fun updateAgentStatus(status: AgentStatus) {
        try {
            val response = service.updateAgentStatus(status.code)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: IOException) {
            // handle exception or rethrow it
        }
    }

    suspend fun updateAgent(params: PutAgentParams) {
        try {
            val response = service.updateAgent(params)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: IOException) {
            // handle exception or rethrow it
        }
    }

    suspend fun getProfile() {
        try {
            val response = service.getProfile()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: IOException) {
            // handle exception or rethrow it
        }
    }

    suspend fun getListQueues(): List<ListQueuesContent>? {
        return try {
            val response = service.getListQueues()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()?.content
        } catch (ex: IOException) {
            null // handle exception or rethrow it
        }
    }
    suspend fun autoLogin() {
        try {
            val response = service.autoLogin()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: IOException) {
            // handle exception or rethrow it
        }
    }

    suspend fun logout(): String? {
        return try {
            val response = service.logout()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()
        } catch (ex: IOException) {
            null // handle exception or rethrow it
        }
    }

    suspend fun autoLogout(): String? {
        return try {
            val response = service.autoLogout()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()
        } catch (ex: IOException) {
            null // handle exception or rethrow it
        }
    }
    suspend fun resetPassword(userName: String) {
        try {
            val response = service.resetPassword(userName)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: IOException) {
            // handle exception or rethrow it
        }
    }

    suspend fun getAgents(skip: Int = 0): List<ZiwoApiGetAgentsContent>? {
        return try {
            val response = service.getAgents(skip)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()?.content
        } catch (ex: IOException) {
            null // handle exception or rethrow it
        }
    }

    suspend fun getCountries(): List<ZiwoApiCountriesContent>? {
        return try {
            val response = service.getCountries()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()?.content
        } catch (ex: IOException) {
            null // handle exception or rethrow it
        }
    }

    suspend fun getAgentCalls(
        skip: Int = 0,
        direction: CallDirection = CallDirection.ALL,
        fromDate: String? = null, //2016-02-04
        toDate: String? = null, //2016-02-04
    ): AgentCall? {
        var requestDirection:String?=null
        var requestLimit:Int?=null
        var requestFromDate: String? =null
        var requestToDate: String? =null
        var skipRequest:Int?=null
        var abandoned:Boolean? = null

        when(direction){
            CallDirection.INBOUND -> {
                requestDirection=direction.value
                abandoned=false
            }
            CallDirection.OUTBOUND -> {
                requestDirection=direction.value
                abandoned=false
            }
            CallDirection.MISSED -> {
                abandoned=true
            }
            else ->{}
        }

        if ( skip > 0){
            skipRequest=skip
        }
        requestFromDate = fromDate ?: "1990-01-01"
        requestToDate = toDate ?: "2050-01-01"
        requestLimit=20

        return try {
            val response = service.getAgentCalls(skipRequest, requestDirection,abandoned, requestFromDate, requestToDate,requestLimit)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()
        } catch (ex: IOException) {
            null // handle exception or rethrow it
        }
    }

    suspend fun getRecordingUrl(callId: String): String? {
        return try {
            val response = service.getRecordingUrl(callId)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            "${response.body()?.content?.endpoint}$callId"
        } catch (ex: IOException) {
            null // handle exception or rethrow it
        }
    }

    suspend fun getStorageEndpoint(): String? {
        return try {
            val response = service.getStorageEndpoint()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            "$baseUrl${response.body()?.content?.endpoint}"
        } catch (ex: IOException) {
            null // handle exception or rethrow it
        }
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