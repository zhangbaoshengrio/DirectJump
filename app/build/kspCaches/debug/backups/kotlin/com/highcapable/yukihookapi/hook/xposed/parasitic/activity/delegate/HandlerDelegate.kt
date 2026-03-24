@file:Suppress("ClassName", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.highcapable.yukihookapi.hook.xposed.parasitic.activity.delegate

import android.os.Handler
import android.os.Message
import androidx.annotation.Keep
import com.highcapable.yukihookapi.hook.xposed.parasitic.activity.delegate.caller.HandlerDelegateCaller

/**
 * HandlerDelegate Class
 *
 * Compiled from YukiHookXposedProcessor
 *
 * Generate Date: Mar 24, 2026, 10:07:44 PM
 *
 * Powered by YukiHookAPI (C) HighCapable 2019-2024
 *
 * Project URL: [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)
 */
@Keep
class HandlerDelegate_com_wizpizz_directjump(private val baseInstance: Handler.Callback?) : Handler.Callback {

    override fun handleMessage(msg: Message) = HandlerDelegateCaller.callHandleMessage(baseInstance, msg)
}