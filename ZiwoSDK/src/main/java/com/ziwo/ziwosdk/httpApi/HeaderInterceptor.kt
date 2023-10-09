package com.ziwo.ziwosdk.httpApi

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit

class HeaderInterceptor(accessToken:String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithHeaders = originalRequest.newBuilder()
            .header("access_token", "AnotherValue")
            .build()
        return chain.proceed(requestWithHeaders)
    }
}


