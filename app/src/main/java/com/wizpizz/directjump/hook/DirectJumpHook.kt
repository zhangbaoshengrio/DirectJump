package com.wizpizz.directjump.hook

import android.content.Context
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

            // ── startActivity hooks (Custom Tabs / explicit intents) ───────────
            hookStartActivity(android.app.Activity::class.java, withBundle = false, rules)
            hookStartActivity(android.app.Activity::class.java, withBundle = true,  rules)
            hookStartActivity(ContextWrapper::class.java,       withBundle = false, rules)
            hookStartActivity(ContextWrapper::class.java,       withBundle = true,  rules)

            // ── WebView hooks (in-app browser / embedded Chromium) ────────────
            // Hook 1: WebView.loadUrl(String)
            WebView::class.java.method {
                name = "loadUrl"
                paramCount = 1
            }.hook {
                before {
                    val url = args[0] as? String ?: return@before
                    val redirected = redirectWebUrl(url, instance as? Context, rules) ?: return@before
                    // Open in external browser and cancel the WebView load
                    instance<Context>().startActivity(redirected)
                    resultNull()
                }
            }

            // Hook 2: WebViewClient.shouldOverrideUrlLoading(WebView, WebResourceRequest)
            WebViewClient::class.java.method {
                name = "shouldOverrideUrlLoading"
                paramCount = 2
            }.hook {
                before {
                    val view = args[0] as? WebView ?: return@before
                    val request = args[1] as? WebResourceRequest ?: return@before
                    val url = request.url?.toString() ?: return@before
                    val ctx = view.context ?: return@before
                    val redirected = redirectWebUrl(url, ctx, rules) ?: return@before
                    ctx.startActivity(redirected)
                    result = true  // tell WebView we handled it
                }
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

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
                redirectIntent(intent, rules)?.let { args[0] = it }
            }
        }
    }

    private fun redirectIntent(intent: Intent, rules: List<RedirectRule>): Intent? {
        if (intent.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        val scheme = uri.scheme ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = runCatching { uri.host }.getOrNull() ?: return null
        val rule = rules.firstOrNull { matchesRule(host, it) } ?: return null

        return buildRedirectIntent(uri, rule).also {
            Log.d(TAG, "[${rule.name}] startActivity → ${rule.targetPkg ?: "default browser"} | $uri")
        }
    }

    private fun redirectWebUrl(url: String, ctx: Context?, rules: List<RedirectRule>): Intent? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val scheme = uri.scheme ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host ?: return null
        val rule = rules.firstOrNull { matchesRule(host, it) } ?: return null

        return buildRedirectIntent(uri, rule).also {
            Log.d(TAG, "[${rule.name}] WebView → ${rule.targetPkg ?: "default browser"} | $url")
        }
    }

    private fun buildRedirectIntent(uri: Uri, rule: RedirectRule): Intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
            rule.targetPkg?.let { setPackage(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun matchesRule(host: String, rule: RedirectRule): Boolean {
        if (rule.excludeHosts.any { ex -> host == ex || host.endsWith(".$ex") }) return false
        if (rule.hosts.contains("*")) return true
        return rule.hosts.any { h -> host == h || host.endsWith(".$h") }
    }
}
