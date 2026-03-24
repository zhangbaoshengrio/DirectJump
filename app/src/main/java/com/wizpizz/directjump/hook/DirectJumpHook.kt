package com.wizpizz.directjump.hook

import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.wizpizz.directjump.config.RedirectRule

object DirectJumpHook {

    private const val TAG = "DirectJump"

    fun apply(packageParam: PackageParam, rules: List<RedirectRule>) {
        packageParam.apply {

            // startActivity hooks (covers Custom Tabs and explicit intents)
            hookStartActivity(android.app.Activity::class.java, withBundle = false, rules)
            hookStartActivity(android.app.Activity::class.java, withBundle = true,  rules)
            hookStartActivity(ContextWrapper::class.java,       withBundle = false, rules)
            hookStartActivity(ContextWrapper::class.java,       withBundle = true,  rules)

            // WebView hooks (covers in-app browsers)
            WebView::class.java.method {
                name = "loadUrl"
                paramCount = 1
            }.hook {
                before {
                    val url = args[0] as? String ?: return@before
                    buildRedirectIntent(url, rules) ?: return@before
                    instance<android.content.Context>().startActivity(
                        buildRedirectIntent(url, rules)!!
                    )
                    resultNull()
                }
            }

            WebViewClient::class.java.method {
                name = "shouldOverrideUrlLoading"
                paramCount = 2
            }.hook {
                before {
                    val view = args[0] as? WebView ?: return@before
                    val request = args[1] as? WebResourceRequest ?: return@before
                    val intent = buildRedirectIntent(request.url?.toString() ?: return@before, rules)
                        ?: return@before
                    view.context.startActivity(intent)
                    result = true
                }
            }
        }
    }

    private fun PackageParam.hookStartActivity(
        clazz: Class<*>,
        withBundle: Boolean,
        rules: List<RedirectRule>
    ) {
        clazz.method {
            name = "startActivity"
            if (withBundle) paramCount = 2 else param(IntentClass)
        }.hook {
            before {
                val intent = args[0] as? Intent ?: return@before
                if (intent.action != Intent.ACTION_VIEW) return@before
                val url = intent.data?.toString() ?: return@before
                buildRedirectIntent(url, rules)?.let { args[0] = it }
            }
        }
    }

    private fun buildRedirectIntent(url: String, rules: List<RedirectRule>): Intent? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val scheme = uri.scheme ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host ?: return null
        val path = uri.path ?: ""

        val rule = rules.firstOrNull { matchesRule(host, path, it) } ?: return null

        // Apply URL transformer (e.g. extract youtube.com/redirect?q=REAL_URL)
        val finalUrl = if (rule.urlTransformer != null) {
            rule.urlTransformer.invoke(url) ?: return null
        } else url

        val finalUri = runCatching { Uri.parse(finalUrl) }.getOrNull() ?: return null

        Log.d(TAG, "[${rule.name}] $url → ${rule.targetPkg ?: "default browser"} (final: $finalUrl)")

        return Intent(Intent.ACTION_VIEW, finalUri).apply {
            rule.targetPkg?.let { setPackage(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun matchesRule(host: String, path: String, rule: RedirectRule): Boolean {
        if (rule.excludeHosts.any { ex -> host == ex || host.endsWith(".$ex") }) return false
        val hostMatches = rule.hosts.contains("*") ||
                rule.hosts.any { h -> host == h || host.endsWith(".$h") }
        if (!hostMatches) return false
        if (rule.pathPrefixes.isEmpty()) return true
        return rule.pathPrefixes.any { path.startsWith(it) }
    }
}
