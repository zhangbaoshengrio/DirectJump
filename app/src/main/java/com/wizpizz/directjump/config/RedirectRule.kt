package com.wizpizz.directjump.config

/**
 * Defines a URL → App redirect rule.
 *
 * @param hosts     Set of domain suffixes to match (e.g. "jd.com" also matches "item.jd.com")
 * @param targetPkg Package name of the app to open instead of the browser
 * @param name      Human-readable name for logging
 */
data class RedirectRule(
    val hosts: Set<String>,
    val targetPkg: String,
    val name: String
)
