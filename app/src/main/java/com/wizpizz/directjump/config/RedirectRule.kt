package com.wizpizz.directjump.config

/**
 * Defines a URL → App redirect rule.
 *
 * @param hosts        Domain suffixes to match (e.g. "jd.com" also matches "item.jd.com").
 *                     Pass setOf("*") to match ALL http/https URLs.
 * @param excludeHosts Domain suffixes to never redirect (takes precedence over [hosts]).
 * @param targetPkg    Package name of the app to open. Pass null to strip any forced
 *                     package and let Android route to the user's default browser.
 * @param name         Human-readable label used in log output.
 */
data class RedirectRule(
    val hosts: Set<String>,
    val excludeHosts: Set<String> = emptySet(),
    val targetPkg: String?,
    val name: String
)
