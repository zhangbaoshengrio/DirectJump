package com.wizpizz.directjump.config

import android.net.Uri

object RedirectConfig {

    // ─── Redirect rules ───────────────────────────────────────────────────────

    /** JD.com links → JD App */
    val RULE_JD = RedirectRule(
        hosts = setOf("item.jd.com", "pro.m.jd.com", "u.jd.com", "union.jd.com", "3.cn", "jd.com"),
        targetPkg = "com.jingdong.app.mall",
        name = "JD"
    )

    /**
     * YouTube description links use https://www.youtube.com/redirect?q=REAL_URL.
     * Extract the "q" parameter and open it in the default browser.
     */
    val RULE_YOUTUBE_REDIRECT = RedirectRule(
        hosts = setOf("youtube.com"),
        pathPrefixes = setOf("/redirect"),
        targetPkg = null,
        name = "YouTubeRedirect",
        urlTransformer = { url ->
            Uri.parse(url).getQueryParameter("q")
        }
    )

    // Add more rules here, e.g.:
    // val RULE_TAOBAO = RedirectRule(
    //     hosts = setOf("item.taobao.com", "taobao.com", "tb.cn"),
    //     targetPkg = "com.taobao.taobao",
    //     name = "Taobao"
    // )

    // ─── Source apps ─────────────────────────────────────────────────────────

    val apps = listOf(

        AppConfig(
            packageName = "com.exinone.exinearn",
            rules = listOf(RULE_JD)
        ),

        AppConfig(
            packageName = "com.google.android.youtube",
            rules = listOf(RULE_YOUTUBE_REDIRECT)
        )
    )
}
