package com.ziwo.ziwosdk.httpApi

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class CacheControlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Check if the response is from cache
        if (response.cacheResponse != null) {
            val sentRequestAtMillis = response.sentRequestAtMillis
            val nowMillis = System.currentTimeMillis()
            val ageMillis = nowMillis - sentRequestAtMillis

            if (ageMillis > TimeUnit.MINUTES.toMillis(5)) {
                // The response is older than 5 minutes, force a network request
                val newRequest = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()
                return chain.proceed(newRequest)
            }
        }

        return response
    }
}
