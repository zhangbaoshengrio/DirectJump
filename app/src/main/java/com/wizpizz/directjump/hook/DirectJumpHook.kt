package com.wizpizz.directjump.hook

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.android.BundleClass
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.wizpizz.directjump.config.RedirectRule

object DirectJumpHook {

    private const val TAG = "DirectJump"

    fun apply(packageParam: PackageParam, rules: List<RedirectRule>) {
        packageParam.apply {

            // Hook 1: Activity.startActivity(Intent)
            // Hook 2: Activity.startActivity(Intent, Bundle)
            // Hook 3: ContextWrapper.startActivity(Intent)  ← catches Fragment / non-Activity calls
            // Hook 4: ContextWrapper.startActivity(Intent, Bundle)

            val handler: (Intent) -> Intent? = { intent -> redirectIfMatched(intent, rules) }

            hookStartActivity(
                clazz = android.app.Activity::class.java,
                withBundle = false,
                handler = handler
            )
            hookStartActivity(
                clazz = android.app.Activity::class.java,
                withBundle = true,
                handler = handler
            )
            hookStartActivity(
                clazz = ContextWrapper::class.java,
                withBundle = false,
                handler = handler
            )
            hookStartActivity(
                clazz = ContextWrapper::class.java,
                withBundle = true,
                handler = handler
            )
        }
    }

    private fun PackageParam.hookStartActivity(
        clazz: Class<*>,
        withBundle: Boolean,
        handler: (Intent) -> Intent?
    ) {
        clazz.method {
            name = "startActivity"
            if (withBundle) paramCount = 2 else param(IntentClass)
        }.hook {
            before {
                val intent = args[0] as? Intent ?: return@before
                handler(intent)?.let { args[0] = it }
            }
        }
    }

    private fun redirectIfMatched(intent: Intent, rules: List<RedirectRule>): Intent? {
        if (intent.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        val scheme = uri.scheme ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = runCatching { uri.host }.getOrNull() ?: return null

        val rule = rules.firstOrNull { matchesRule(host, it) } ?: return null

        return when {
            rule.targetPkg == null -> {
                // Default browser: create a clean intent, dropping any forced package.
                // This handles both cases:
                //   • YouTube sets package = "com.android.chrome" (Custom Tabs)
                //   • YouTube leaves package = null (direct ACTION_VIEW)
                // Either way we fire a fresh intent so Android routes it to the
                // user's default browser.
                Log.d(TAG, "[${rule.name}] → default browser | pkg was='${intent.`package`}' url=$uri")
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            else -> {
                Log.d(TAG, "[${rule.name}] → ${rule.targetPkg} | url=$uri")
                Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(rule.targetPkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }

    private fun matchesRule(host: String, rule: RedirectRule): Boolean {
        if (rule.excludeHosts.any { ex -> host == ex || host.endsWith(".$ex") }) return false
        if (rule.hosts.contains("*")) return true
        return rule.hosts.any { h -> host == h || host.endsWith(".$h") }
    }
}
