package com.wizpizz.directjump.config

/**
 * Central registry of all redirect rules.
 *
 * To add a new source app:  add an AppConfig entry to [apps].
 * To add a new redirect target: define a new RedirectRule constant and reference it in [apps].
 */
object RedirectConfig {

    // ─── Redirect rules ───────────────────────────────────────────────────────

    /** JD.com links → JD App */
    val RULE_JD = RedirectRule(
        hosts = setOf("item.jd.com", "pro.m.jd.com", "u.jd.com", "union.jd.com", "3.cn", "jd.com"),
        targetPkg = "com.jingdong.app.mall",
        name = "JD"
    )

    /**
     * Any http/https link → default browser.
     * Excludes YouTube's own domains so internal navigation stays in-app.
     * targetPkg = null means "strip forced package, let Android pick default browser".
     */
    val RULE_DEFAULT_BROWSER = RedirectRule(
        hosts = setOf("*"),
        excludeHosts = setOf("youtube.com", "youtu.be", "googlevideo.com", "ytimg.com", "ggpht.com"),
        targetPkg = null,
        name = "DefaultBrowser"
    )

    // Add more rules here, e.g.:
    // val RULE_TAOBAO = RedirectRule(
    //     hosts = setOf("item.taobao.com", "taobao.com", "tb.cn"),
    //     targetPkg = "com.taobao.taobao",
    //     name = "Taobao"
    // )

    // ─── Source apps ─────────────────────────────────────────────────────────

    val apps = listOf(

        // 水龙头 — 返利 / 优惠券聚合 App
        AppConfig(
            packageName = "com.exinone.exinearn",
            rules = listOf(RULE_JD)
        ),

        // YouTube — open description links in default browser instead of Custom Tabs
        AppConfig(
            packageName = "com.google.android.youtube",
            rules = listOf(RULE_DEFAULT_BROWSER)
        )

        // Add more source apps here, e.g.:
        // AppConfig(
        //     packageName = "com.example.otherapp",
        //     rules = listOf(RULE_JD, RULE_DEFAULT_BROWSER)
        // )
    )
}
