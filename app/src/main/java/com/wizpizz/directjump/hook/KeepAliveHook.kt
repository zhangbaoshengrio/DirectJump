package com.wizpizz.directjump.hook

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.util.Log
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam

object KeepAliveHook {

    private const val TAG = "DirectJump"
    private const val CHANNEL_ID = "keepalive"
    private const val NOTIF_ID = 9527

    @Volatile private var done = false

    fun apply(packageParam: PackageParam) {
        packageParam.apply {
            Service::class.java.method {
                name = "onCreate"
                emptyParam()
            }.hook {
                after {
                    if (done) return@after
                    tryStartForeground(instance())
                }
            }
        }
    }

    @Synchronized
    private fun tryStartForeground(svc: Service) {
        if (done) return
        // 只在主进程生效，排除 :push / :xweb 等子进程
        val proc = runCatching {
            java.io.File("/proc/self/cmdline").readText().trimEnd('\u0000')
        }.getOrDefault("")
        if (proc != "com.tencent.mm") return

        runCatching {
            val nm = svc.getSystemService(NotificationManager::class.java)!!
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "后台保活", NotificationManager.IMPORTANCE_MIN)
                    .also { it.setShowBadge(false); it.setSound(null, null) }
            )
            svc.startForeground(
                NOTIF_ID,
                Notification.Builder(svc, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                    .setContentTitle("微信")
                    .setContentText("后台运行中")
                    .setOngoing(true)
                    .build()
            )
            done = true
            Log.d(TAG, "KeepAlive: foreground started via ${svc.javaClass.simpleName}")
        }.onFailure {
            Log.w(TAG, "KeepAlive: ${it.message}")
        }
    }
}
