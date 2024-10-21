package com.cwuom.ouo.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import kotlinx.serialization.Serializable
import mqq.app.MobileQQ
import kotlin.random.Random


internal object PlatformUtils {
    fun getQUA(): String {
        return "V1_AND_SQ_${getQQVersion(MobileQQ.getContext())}_${getQQVersionCode()}_YYB_D"
    }

    fun getQQVersion(context: Context): String {
        val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionName
    }

    fun getQQVersionCode(context: Context = MobileQQ.getContext()): Int {
        val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionCode
    }

    fun requireMinQQVersion(context: Context = MobileQQ.getContext(), version: Int) {
        require(getQQVersionCode(context) >= version) {
            "require QQ version >= $version, but current version is ${getQQVersionCode(context)}"
        }
    }

    /**
     * 获取OIDB包的ClientVersion信息
     */
    fun getClientVersion(context: Context): String = "android ${getQQVersion(context)}"

    /**
     * 是否处于QQ MSF协议进程
     */
    fun isMsfProcess(): Boolean {
        return MobileQQ.getMobileQQ().qqProcessName.contains("msf", ignoreCase = true)
    }

    /**
     * 是否处于QQ主进程
     */
    fun isMainProcess(): Boolean {
        return isMqq() || isTim()
    }

    fun isMqq(): Boolean {
        return MobileQQ.getMobileQQ().qqProcessName == "com.tencent.mobileqq"
    }

    fun isMqqPackage(): Boolean {
        return MobileQQ.getMobileQQ().qqProcessName.startsWith("com.tencent.mobileqq")
    }

    fun isTim(): Boolean {
        return MobileQQ.getMobileQQ().qqProcessName == "com.tencent.tim"
    }

    fun isApkInDebug(context: Context): Boolean {
        try {
            val info = context.applicationInfo
            return (info.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            return false
        }
    }

    fun killProcess(context: Context, processName: String) {
        for (processInfo in (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses) {
            if (processInfo.processName == processName) {
                Process.killProcess(processInfo.pid)
            }
        }
    }

    fun getDeviceBattery(): DeviceBattery {
        val ctx = MobileQQ.getContext()
        return kotlin.runCatching {
            val batteryManager = ctx.getSystemService(BATTERY_SERVICE) as BatteryManager

            DeviceBattery(
                battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
                scale = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
                status = batteryManager.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_STATUS
                ),
            )
        }.getOrElse {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val receiver = ctx.registerReceiver(null, filter)
            DeviceBattery(
                battery = receiver?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1,
                scale = receiver?.getIntExtra("scale", 0) ?: -1,
                status = receiver?.getIntExtra("status", 0) ?: -1
            )
        }
    }

    @SuppressLint("HardwareIds")
    fun getAndroidID(): String {
        var androidId =
            Settings.Secure.getString(MobileQQ.getContext().contentResolver, "android_id")
        if (androidId == null) {
            val sb = StringBuilder()
            for (i in 0..15) {
                sb.append(Random.nextInt(10))
            }
            androidId = sb.toString()
        }
        return androidId
    }

    @Serializable
    data class DeviceBattery(
        val battery: Int,
        val scale: Int,
        val status: Int
    )
}