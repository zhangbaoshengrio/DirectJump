package com.wizpizz.directjump.hook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.wizpizz.directjump.config.RedirectRule

object DirectJumpHook {

    private const val TAG = "DirectJump"

    fun apply(packageParam: PackageParam, rules: List<RedirectRule>) {
        packageParam.apply {
            Activity::class.java.method {
                name = "startActivity"
                param(IntentClass)
            }.hook {
                before {
                    val intent = args[0] as? Intent ?: return@before
                    redirectIfMatched(intent, instance as Activity, rules)?.let { args[0] = it }
                }
            }

            Activity::class.java.method {
                name = "startActivity"
                paramCount = 2
            }.hook {
                before {
                    val intent = args[0] as? Intent ?: return@before
                    redirectIfMatched(intent, instance as Activity, rules)?.let { args[0] = it }
                }
            }
        }
    }

    private fun redirectIfMatched(intent: Intent, activity: Activity, rules: List<RedirectRule>): Intent? {
        if (intent.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        val scheme = uri.scheme ?: return null
        if (scheme != "http" && scheme != "https") return null

        val host = runCatching { uri.host }.getOrNull() ?: return null

        val rule = rules.firstOrNull { rule -> matchesRule(host, rule) } ?: return null

        // targetPkg == null → default browser (strip forced package)
        if (rule.targetPkg == null) {
            // Only redirect if YouTube (or another app) has locked the intent to a specific browser
            if (intent.`package` == null) return null
            Log.d(TAG, "[${rule.name}] Stripping forced package '${intent.`package`}', opening $uri in default browser")
            return Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // targetPkg set → redirect to that specific app
        val installed = activity.packageManager.getLaunchIntentForPackage(rule.targetPkg) != null
        if (!installed) {
            Log.w(TAG, "[${rule.name}] App ${rule.targetPkg} not installed, skipping redirect")
            return null
        }
        Log.d(TAG, "[${rule.name}] Redirecting $uri → ${rule.targetPkg}")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(rule.targetPkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun matchesRule(host: String, rule: RedirectRule): Boolean {
        // Check exclusions first
        if (rule.excludeHosts.any { ex -> host == ex || host.endsWith(".$ex") }) return false
        // Wildcard matches all hosts
        if (rule.hosts.contains("*")) return true
        return rule.hosts.any { h -> host == h || host.endsWith(".$h") }
    }
}
