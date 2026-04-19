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
    private val redirectCache = mutableMapOf<String, String>()

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

        // For short-link hosts, resolve to real product URL so JD app can open it directly.
        val finalUrl = if (rule.resolveRedirect && isShortLink(transformedUrl)) {
            redirectCache.getOrPut(transformedUrl) { resolveRedirect(transformedUrl) }
                .also { Log.d(TAG, "[${rule.name}] resolved $transformedUrl → $it") }
        } else transformedUrl

        // If resolution yielded an openapp.jdmobile:// deep link, use it directly
        if (finalUrl.startsWith("openapp.jdmobile://")) {
            Log.d(TAG, "[${rule.name}] using extracted deep link: $finalUrl")
            return Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                setPackage("com.jingdong.app.mall")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // Convert item.jd.com product URLs to JD's native deep link scheme,
        // which is the only reliable way to open a specific product in JD app.
        val jdDeepLink = toJdDeepLink(finalUrl)
        if (jdDeepLink != null) {
            Log.d(TAG, "[${rule.name}] deep link $finalUrl → $jdDeepLink")
            return Intent(Intent.ACTION_VIEW, Uri.parse(jdDeepLink)).apply {
                setPackage("com.jingdong.app.mall")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        val finalUri = runCatching { Uri.parse(finalUrl) }.getOrNull() ?: return null
        val targetPkg = if (isShortLink(finalUrl)) null else rule.targetPkg

        Log.d(TAG, "[${rule.name}] $url → ${targetPkg ?: "app-links"} (final: $finalUrl)")

        return Intent(Intent.ACTION_VIEW, finalUri).apply {
            targetPkg?.let { setPackage(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private val JD_SKU_RE = Regex("""/(?:product/)?(\d+)\.html""")

    /** Convert a JD product/jingfen URL to JD's native deep link scheme. */
    private fun toJdDeepLink(url: String): String? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        return when {
            host == "item.jd.com" || host == "pro.m.jd.com" -> {
                val skuId = JD_SKU_RE.find(uri.path ?: return null)?.groupValues?.get(1) ?: return null
                """openapp.jdmobile://virtual?params={"category":"jump","des":"productDetail","skuId":"$skuId"}"""
            }
            host == "jingfen.jd.com" -> {
                // Open jingfen affiliate page in JD app's built-in browser
                val urlForJson = url.replace("\\", "\\\\").replace("\"", "\\\"")
                """openapp.jdmobile://virtual?params={"category":"jump","des":"h5","url":"$urlForJson"}"""
            }
            else -> null
        }
    }

    private fun isShortLink(url: String): Boolean {
        val host = runCatching { Uri.parse(url).host }.getOrNull() ?: return false
        return host == "3.cn" || host == "u.jd.com" || host == "jingfen.jd.com"
    }

    private val HRL_RE = Regex("""var hrl='([^']+)'""")
    private val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36"

    /**
     * Resolve a JD short URL (u.jd.com / 3.cn) to the real item.jd.com product URL.
     *
     * Two-step process:
     *  1. GET the short-link page (returns 200 HTML) → extract the `hrl` JS variable
     *     which points to u.jd.com/jda?p=<encoded>
     *  2. HEAD the `hrl` URL → returns 302 Location: https://item.jd.com/XXXXX.html
     */
    private fun resolveRedirect(url: String): String {
        val latch = CountDownLatch(1)
        var resolved = url
        Thread {
            try {
                // Step 1: fetch short-link page and extract hrl
                val step1 = URL(url).openConnection() as HttpURLConnection
                step1.instanceFollowRedirects = false
                step1.requestMethod = "GET"
                step1.connectTimeout = 5000
                step1.readTimeout = 5000
                step1.setRequestProperty("User-Agent", UA)
                step1.connect()
                val code1 = step1.responseCode
                val loc1 = step1.getHeaderField("Location")
                Log.d(TAG, "resolveRedirect step1 $url → code=$code1 location=$loc1")

                val hrl: String? = if (loc1 != null) {
                    loc1  // plain HTTP redirect (3.cn might do this)
                } else if (code1 == 200) {
                    val html = step1.inputStream.bufferedReader(Charsets.UTF_8).readText()
                    HRL_RE.find(html)?.groupValues?.get(1)?.also {
                        Log.d(TAG, "resolveRedirect step1 extracted hrl: $it")
                    }
                } else null
                step1.disconnect()

                if (hrl == null) {
                    Log.w(TAG, "resolveRedirect: could not extract hrl from $url")
                    return@Thread
                }

                // Step 2: follow hrl → should 302 to item.jd.com
                val step2 = URL(hrl).openConnection() as HttpURLConnection
                step2.instanceFollowRedirects = false
                step2.requestMethod = "GET"
                step2.connectTimeout = 5000
                step2.readTimeout = 5000
                step2.setRequestProperty("User-Agent", UA)
                step2.connect()
                val code2 = step2.responseCode
                val loc2 = step2.getHeaderField("Location")
                Log.d(TAG, "resolveRedirect step2 $hrl → code=$code2 location=$loc2")
                step2.disconnect()

                if (loc2 != null) resolved = loc2

                // Step 3: if resolved to jingfen.jd.com, extract openapp:// deep link from HTML
                val resolvedHost = runCatching { Uri.parse(resolved).host }.getOrNull()
                if (resolvedHost == "jingfen.jd.com") {
                    val step3 = URL(resolved).openConnection() as HttpURLConnection
                    step3.instanceFollowRedirects = false
                    step3.requestMethod = "GET"
                    step3.connectTimeout = 5000
                    step3.readTimeout = 5000
                    step3.setRequestProperty("User-Agent", UA)
                    step3.connect()
                    val code3 = step3.responseCode
                    val loc3 = step3.getHeaderField("Location")
                    Log.d(TAG, "resolveRedirect step3 $resolved → code=$code3 location=$loc3")
                    if (loc3 != null) {
                        resolved = loc3
                    } else if (code3 == 200) {
                        val html3 = step3.inputStream.bufferedReader(Charsets.UTF_8).readText()
                        // First try item.jd.com URL
                        val itemUrl = Regex("""https://(?:item|pro\.m)\.jd\.com/[^\s"'<]+""").find(html3)?.value
                        if (itemUrl != null) {
                            resolved = itemUrl
                            Log.d(TAG, "resolveRedirect step3 found item URL: $itemUrl")
                        } else {
                            // Fall back to openapp.jdmobile:// deep link embedded in the page
                            Regex("""openapp\.jdmobile://[^\s"'\\<]+""").find(html3)?.value?.also {
                                resolved = it
                                Log.d(TAG, "resolveRedirect step3 found openapp URL: $it")
                            }
                        }
                    }
                    step3.disconnect()
                }

            } catch (e: Exception) {
                Log.w(TAG, "resolveRedirect failed: ${e.message}")
            } finally {
                latch.countDown()
            }
        }.start()
        latch.await(12, TimeUnit.SECONDS)
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
