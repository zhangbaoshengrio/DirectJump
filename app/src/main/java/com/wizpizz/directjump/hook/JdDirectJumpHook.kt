package com.wizpizz.directjump.hook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.android.IntentClass

object JdDirectJumpHook {

    private const val TAG = "DirectJump"
    private const val JD_PACKAGE = "com.jingdong.app.mall"

    private val JD_HOSTS = setOf(
        "item.jd.com",
        "pro.m.jd.com",
        "u.jd.com",
        "union.jd.com",
        "3.cn",
        "jd.com"
    )

    fun apply(packageParam: PackageParam) {
        packageParam.apply {
            // Hook startActivity(Intent)
            Activity::class.java.method {
                name = "startActivity"
                param(IntentClass)
            }.hook {
                before {
                    val intent = args[0] as? Intent ?: return@before
                    redirectIfJd(intent, instance as Activity)?.let { args[0] = it }
                }
            }

            // Hook startActivity(Intent, Bundle)
            Activity::class.java.method {
                name = "startActivity"
                paramCount = 2
            }.hook {
                before {
                    val intent = args[0] as? Intent ?: return@before
                    redirectIfJd(intent, instance as Activity)?.let { args[0] = it }
                }
            }
        }
    }

    private fun redirectIfJd(intent: Intent, activity: Activity): Intent? {
        if (intent.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        val url = uri.toString()

        if (!isJdUrl(url)) return null

        val jdInstalled = activity.packageManager.getLaunchIntentForPackage(JD_PACKAGE) != null
        if (!jdInstalled) {
            Log.w(TAG, "JD App 未安装，保持原始跳转: $url")
            return null
        }

        Log.d(TAG, "拦截京东链接，直跳 JD App: $url")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(JD_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun isJdUrl(url: String): Boolean {
        return try {
            val host = Uri.parse(url).host ?: return false
            JD_HOSTS.any { host == it || host.endsWith(".$it") }
        } catch (e: Exception) {
            false
        }
    }
}
