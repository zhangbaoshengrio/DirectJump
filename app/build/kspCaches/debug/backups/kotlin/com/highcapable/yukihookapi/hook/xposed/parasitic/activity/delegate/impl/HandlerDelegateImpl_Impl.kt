@file:Suppress("ClassName", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.highcapable.yukihookapi.hook.xposed.parasitic.activity.delegate.impl

import android.os.Handler
import com.highcapable.yukihookapi.hook.xposed.parasitic.activity.delegate.HandlerDelegate_com_wizpizz_directjump

/**
 * HandlerDelegateImpl_Impl Class
 *
 * Compiled from YukiHookXposedProcessor
 *
 * Generate Date: Mar 24, 2026, 8:16:31 PM
 *
 * Powered by YukiHookAPI (C) HighCapable 2019-2024
 *
 * Project URL: [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)
 */
object HandlerDelegateImpl_Impl {

    val wrapperClassName get() = "com.highcapable.yukihookapi.hook.xposed.parasitic.activity.delegate.HandlerDelegate_com_wizpizz_directjump"

    fun createWrapper(baseInstance: Handler.Callback? = null): Handler.Callback = HandlerDelegate_com_wizpizz_directjump(baseInstance)
}