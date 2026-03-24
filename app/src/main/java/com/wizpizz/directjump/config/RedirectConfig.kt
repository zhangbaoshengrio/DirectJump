package com.wizpizz.directjump.config

/**
 * Central registry of all redirect rules.
 *
 * To add a new source app:  add an AppConfig entry to [apps].
 * To add a new redirect target: define a new RedirectRule constant and reference it in [apps].
 */
object RedirectConfig {

    // ─── Redirect rules (URL pattern → target app) ───────────────────────────

    val RULE_JD = RedirectRule(
        hosts = setOf("item.jd.com", "pro.m.jd.com", "u.jd.com", "union.jd.com", "3.cn", "jd.com"),
        targetPkg = "com.jingdong.app.mall",
        name = "JD"
    )

    // Add more rules here, e.g.:
    // val RULE_TAOBAO = RedirectRule(
    //     hosts = setOf("item.taobao.com", "taobao.com", "tb.cn"),
    //     targetPkg = "com.taobao.taobao",
    //     name = "Taobao"
    // )

    // ─── Source apps and which rules apply to each ────────────────────────────

    val apps = listOf(

        // 水龙头 — 返利 / 优惠券聚合 App
        AppConfig(
            packageName = "com.exinone.exinearn",
            rules = listOf(RULE_JD)
        )

        // Add more source apps here, e.g.:
        // AppConfig(
        //     packageName = "com.example.otherapp",
        //     rules = listOf(RULE_JD, RULE_TAOBAO)
        // )
    )
}
