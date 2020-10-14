package com.ziwo.ziwosdk

import android.content.Context
import com.ziwo.ziwosdk.httpApi.ZiwoApi
import com.ziwo.ziwosdk.socketApi.ZiwoWsApi
import com.ziwo.agent.utils.ziwoSdk.verto.VertoWs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Ziwo  @Inject constructor(
    @ApplicationContext var context: Context
)  {

    // class logic
    private val TAG = "ZiwoMotherClient"
    @Inject lateinit var ziwoApiClient: ZiwoApi
    public var vertoWs = VertoWs(context, this)
    public var ziwoApiWs = ZiwoWsApi(context, this)

}