@file:Suppress("ClassName", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.highcapable.yukihookapi.hook.xposed.parasitic.activity.delegate

import androidx.annotation.Keep
import com.highcapable.yukihookapi.hook.xposed.parasitic.activity.delegate.caller.IActivityManagerProxyCaller
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * IActivityManagerProxy Class
 *
 * Compiled from YukiHookXposedProcessor
 *
 * Generate Date: Mar 24, 2026, 8:16:31 PM
 *
 * Powered by YukiHookAPI (C) HighCapable 2019-2024
 *
 * Project URL: [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)
 */
@Keep
class IActivityManagerProxy_com_wizpizz_directjump(private val baseInstance: Any) : InvocationHandler {

    override fun invoke(proxy: Any?, method: Method?, args: Array<Any>?) = IActivityManagerProxyCaller.callInvoke(baseInstance, method, args)
}