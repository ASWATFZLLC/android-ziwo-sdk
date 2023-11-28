package com.ziwo.ziwosdk.httpApi

import android.content.Context
import android.util.Log
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.gson.Gson
import com.ziwo.ziwosdk.Ziwo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class ZiwoApi(private val appContext: Context, private val ziwo: Ziwo) {

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
    private var client : OkHttpClient? =null
    // Set up Retrofit with OkHttpClient and GsonConverterFactory
    private  var retrofit: Retrofit? =null

    // Instantiate service
    private lateinit var service: ZiwoApiService

    private fun buildClient() {
        val headerInterceptor = HeaderInterceptor(this.accessToken)
        client = OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)  // Increase read timeout
            .connectTimeout(15, TimeUnit.SECONDS)  // Increase connection timeout
            .addInterceptor(loggingInterceptor)
            .addInterceptor(CacheControlInterceptor())
            .addInterceptor( ChuckerInterceptor.Builder(appContext)
                .collector(ChuckerCollector(appContext))
                .maxContentLength(250_000L)
                .alwaysReadResponseBody(true)
                .build())
            .addInterceptor(headerInterceptor)
            .build()
        // Optionally, also rebuild your Retrofit instance to use the new OkHttpClient
        retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(this.baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit?.create(ZiwoApiService::class.java)!!
    }
    private fun updateBaseUrl(callCenter: String){
        this.baseUrl = "https://$callCenter-api.aswat.co"
        buildClient()

    }
    private fun updateAccessToken(accessToken: String){
        this.accessToken = accessToken
        buildClient()
    }

    // Custom Exception
    open class ZiwoException(message: String) : Exception(message)
    class UserIsAdminException(message: String) : ZiwoException(message)

    /**
     * useful for when restoring session manually offline
     */
    fun setCredentials( accessToken: String? = null) {
        if (accessToken != null) {
            updateAccessToken(accessToken)
        }
    }

    suspend fun checkCallCenter(callCenter: String): Boolean {
        updateBaseUrl(callCenter)
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
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun login(callCenter: String, userName: String, userPassword: String, vertoSessionId: String,shouldCheckGeographicAccess:Boolean ): ZiwoApiLoginContentData? {
        if (retrofit==null||service==null)
            updateBaseUrl(callCenter)
        try {
            if (shouldCheckGeographicAccess&&!checkGeographicAccess(callCenter, userName)) {
                throw IOException("Geographic Access Denied")
            }
            val response = service.login(callCenter, userName, userPassword, vertoSessionId)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            if( response.body()?.content?.type != "agent"){
                throw UserIsAdminException("attempt login with non agent type user")
            }
            response.body()?.content?.access_token?.let {
                updateAccessToken(
                    it
                )
            }
            return response.body()?.content
        } catch (ex: Exception) {
            // handle exception or rethrow it
            throw ex
        }
    }
    private suspend fun checkGeographicAccess(callCenter: String, userName: String): Boolean {
        val url = "https://s3gmtzv4z2uel4gy7k5cnjov5a0tehjm.lambda-url.eu-west-3.on.aws/"
        val httpClient = OkHttpClient()
        val request = Request.Builder()
            .url("$url?tenant=$callCenter&lg=$userName")
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "checkAdditionalApi Response: $responseBody")

                // Assuming the response body is a plain text "true" or "false"
                val apiResponse = Gson().fromJson(responseBody, GeographicAccessResponse::class.java)
                apiResponse?.result ?: false
            } else {
                Log.e(TAG, "checkAdditionalApi Error: Response Code ${response.code}")
                false
            }
        } catch (ex: IOException) {
            Log.e(TAG, "checkAdditionalApi Exception: ${ex.message}")
            false
        }
    }
    suspend fun updateAgentStatus(status: AgentStatus) {
        try {
            val response = service.updateAgentStatus(status.code)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun updateAgent(params: PutAgentParams) {
        try {
            val response = service.updateAgent(params)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun getProfile() {
        try {
            val response = service.getProfile()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun getListQueues(): List<ListQueuesContent>? {
        return try {
            val response = service.getListQueues()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()?.content
        } catch (ex: Exception) {
            throw ex
        }
    }
    suspend fun autoLogin() {
        try {
            val requestBody = RequestBody.create(
                "application/json".toMediaType(),
                "{}"
            )
            val response = service.autoLogin(requestBody)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun logout(): String? {
        return try {
            val response = service.logout()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun autoLogout(): String? {
        return try {
            val requestBody = RequestBody.create(
                "application/json".toMediaType(),
                "{}"
            )

            val response = service.autoLogout(requestBody)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()
        } catch (ex: Exception) {
            throw ex
        }
    }
    suspend fun resetPassword(userName: String) {
        try {
            val response = service.resetPassword(userName)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun getAgents(useCache: Boolean =true, skip: Int = 0): List<ZiwoApiGetAgentsContent>? {
        return try {
            val response = service.getAgents(useCache, skip)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()?.content
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun getCountries(): List<ZiwoApiCountriesContent>? {
        return try {
            val response = service.getCountries()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body()?.content
        } catch (ex: Exception) {
            throw ex
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
            throw ex
        }
    }

    suspend fun getRecordingUrl(callId: String): String? {
        return try {
            val response = service.getRecordingUrl(callId)
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            "${response.body()?.content?.endpoint}$callId"
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun getStorageEndpoint(): String? {
        return try {
            val response = service.getStorageEndpoint()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            "$baseUrl${response.body()?.content?.endpoint}"
        } catch (ex: Exception) {
            throw ex
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