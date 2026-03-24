package com.wizpizz.directjump.config

/**
 * Associates a source app with the redirect rules it should apply.
 *
 * @param packageName Source app package name to hook
 * @param rules       List of redirect rules active for this app
 */
data class AppConfig(
    val packageName: String,
    val rules: List<RedirectRule>
)
