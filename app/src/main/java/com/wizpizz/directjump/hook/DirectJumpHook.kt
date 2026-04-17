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
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
            // Hook both loadUrl(String) and loadUrl(String, Map) variants
            for (paramCount in listOf(1, 2)) {
                WebView::class.java.method {
                    name = "loadUrl"
                    this.paramCount = paramCount
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
        val transformedUrl = if (rule.urlTransformer != null) {
            rule.urlTransformer.invoke(url) ?: return null
        } else url

        // For short-link hosts that need resolving (e.g. 3.cn, u.jd.com),
        // follow the redirect to get the real destination URL.
        val finalUrl = if (rule.resolveRedirect && isShortLink(transformedUrl)) {
            resolveRedirect(transformedUrl).also {
                if (it != transformedUrl) Log.d(TAG, "[${rule.name}] resolved $transformedUrl → $it")
            }
        } else transformedUrl

        val finalUri = runCatching { Uri.parse(finalUrl) }.getOrNull() ?: return null

        Log.d(TAG, "[${rule.name}] $url → ${rule.targetPkg ?: "default browser"} (final: $finalUrl)")

        return Intent(Intent.ACTION_VIEW, finalUri).apply {
            rule.targetPkg?.let { setPackage(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun isShortLink(url: String): Boolean {
        val host = runCatching { Uri.parse(url).host }.getOrNull() ?: return false
        return host == "3.cn" || host == "u.jd.com"
    }

    /** Follow HTTP redirects (one hop) on a background thread, timeout 3 s. */
    private fun resolveRedirect(url: String): String {
        val latch = CountDownLatch(1)
        var resolved = url
        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.requestMethod = "HEAD"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connect()
                conn.getHeaderField("Location")?.let { resolved = it }
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "resolveRedirect failed: ${e.message}")
            } finally {
                latch.countDown()
            }
        }.start()
        latch.await(3, TimeUnit.SECONDS)
        return resolved
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
