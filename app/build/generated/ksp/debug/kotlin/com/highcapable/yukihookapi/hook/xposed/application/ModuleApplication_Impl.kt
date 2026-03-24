@file:Suppress("ClassName")

package com.highcapable.yukihookapi.hook.xposed.application

import com.wizpizz.directjump.HookEntry

/**
 * ModuleApplication_Impl Class
 *
 * Compiled from YukiHookXposedProcessor
 *
 * Generate Date: Mar 24, 2026, 10:07:44 PM
 *
 * Powered by YukiHookAPI (C) HighCapable 2019-2024
 *
 * Project URL: [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)
 */
object ModuleApplication_Impl {

    fun callHookEntryInit() = try {
        HookEntry.onInit()
    } catch (_: Throwable) {
    }
}