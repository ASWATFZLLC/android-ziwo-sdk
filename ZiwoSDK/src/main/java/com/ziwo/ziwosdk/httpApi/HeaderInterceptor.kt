package com.ziwo.ziwosdk.httpApi

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit

class HeaderInterceptor(private var accessToken:String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain,): Response {
        val originalRequest = chain.request()
        val requestWithHeaders = originalRequest.newBuilder()
            .header("access_token",accessToken)
            .build()
        Log.d("add accessToken",accessToken)
        return chain.proceed(requestWithHeaders)
    }
}


