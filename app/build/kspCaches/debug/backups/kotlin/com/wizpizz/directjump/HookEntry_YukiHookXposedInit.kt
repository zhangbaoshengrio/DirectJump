@file:Suppress("ClassName", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.wizpizz.directjump

import androidx.annotation.Keep
import com.highcapable.yukihookapi.hook.xposed.bridge.event.caller.YukiXposedEventCaller

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit

import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Xposed Init Class
 *
 * Compiled from YukiHookXposedProcessor
 *
 * Generate Date: Mar 24, 2026, 9:14:57 PM
 *
 * Powered by YukiHookAPI (C) HighCapable 2019-2024
 *
 * Project URL: [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)
 */
@Keep
class HookEntry_YukiHookXposedInit : IXposedHookZygoteInit, IXposedHookLoadPackage {

    override fun initZygote(sparam: IXposedHookZygoteInit.StartupParam?) {
        HookEntry_Impl.callInitZygote(sparam)
        YukiXposedEventCaller.callInitZygote(sparam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        HookEntry_Impl.callHandleLoadPackage(lpparam)
        YukiXposedEventCaller.callHandleLoadPackage(lpparam)
    }
}