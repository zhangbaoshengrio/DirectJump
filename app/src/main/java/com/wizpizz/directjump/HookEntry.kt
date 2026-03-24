package com.wizpizz.directjump

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.wizpizz.directjump.config.RedirectConfig
import com.wizpizz.directjump.hook.DirectJumpHook

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {

    override fun onHook() = encase {
        configs {
            debugLog { tag = "DirectJump" }
        }
        RedirectConfig.apps.forEach { appConfig ->
            loadApp(name = appConfig.packageName) {
                DirectJumpHook.apply(this, appConfig.rules)
            }
        }
    }
}
