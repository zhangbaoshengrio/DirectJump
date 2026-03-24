package com.wizpizz.directjump.config

/**
 * Defines a URL → App redirect rule.
 *
 * @param hosts        Domain suffixes to match. Use setOf("*") for all http/https URLs.
 * @param pathPrefixes If non-empty, the URL path must start with one of these values.
 *                     Leave empty to match any path.
 * @param excludeHosts Domain suffixes to never redirect (takes precedence over [hosts]).
 * @param targetPkg    Package of the app to open. null = strip forced package → default browser.
 * @param name         Label used in log output.
 * @param urlTransformer Optional function to rewrite the URL before opening.
 *                       Return the new URL string, or null to skip the redirect.
 */
data class RedirectRule(
    val hosts: Set<String>,
    val pathPrefixes: Set<String> = emptySet(),
    val excludeHosts: Set<String> = emptySet(),
    val targetPkg: String?,
    val name: String,
    val urlTransformer: ((String) -> String?)? = null
)
