package com.wizpizz.directjump

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.wizpizz.directjump.hook.JdDirectJumpHook

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {

    override fun onHook() = encase {
        configs {
            debugLog { tag = "DirectJump" }
        }
        loadApp(name = "com.exinone.exinearn") {
            JdDirectJumpHook.apply(this)
        }
    }
}
