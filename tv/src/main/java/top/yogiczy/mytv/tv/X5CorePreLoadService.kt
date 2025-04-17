package top.yogiczy.mytv.tv

import android.content.Context
import android.content.Intent
import androidx.annotation.Nullable
import androidx.core.app.JobIntentService
import com.tencent.smtt.sdk.QbSdk
import top.yogiczy.mytv.core.data.utils.Logger
import java.io.File
import java.net.URL
import java.util.concurrent.Executors
import android.os.Build

class X5CorePreLoadService : JobIntentService() {
    private val log = Logger.create("X5CorePreLoadService")
    override fun onHandleWork(@Nullable intent: Intent) {
        // 在这里添加我们要执行的代码，Intent 中可以保存我们所需的数据，
        // 每一次通过 Intent 发送的命令将被顺序执行
        initX5()
    }

    /**
     * 初始化 X5 内核
     */
    private fun initX5() {
        val apkName = "TBScore.apk"
        val apkDir = applicationContext.filesDir.absolutePath
        val apkPath = "$apkDir/$apkName"
        val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val downloadUrl = when (arch) {
            "arm64-v8a" -> "https://gitee.com/mytv-android/mytv_lib/releases/download/V1.0.0/046007_x5.tbs.apk"
            "armeabi-v7a" -> "https://gitee.com/mytv-android/mytv_lib/releases/download/V1.0.0/045912_x5.tbs.apk"
            else -> null
        }
        val code = when (arch) {
            "arm64-v8a" -> 46007
            "armeabi-v7a" -> 45912
            else -> 0
        }
        if (downloadUrl != null) {
            Executors.newSingleThreadExecutor().execute {
                try {
                    // 下载 APK 文件
                    val url = URL(downloadUrl)
                    val connection = url.openConnection()
                    connection.connect()
                    val inputStream = connection.getInputStream()
                    val file = File(apkPath)
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    // 加载本地 TBS 内核
                    QbSdk.installLocalTbsCore(applicationContext, code, apkPath)
                    log.i("Core APK 下载并加载成功: $apkPath")
                } catch (e: Exception) {
                    log.e("Core APK 下载或加载失败: ${e.message}")
                }
            }
        } else {
            log.e("不支持的架构: $arch")
        }
    }

    companion object {
        private val TAG = X5CorePreLoadService::class.java.simpleName
        /**
         * 启动服务的便捷方法
         */
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, X5CorePreLoadService::class.java, JOB_ID, intent)
        }
        private const val JOB_ID = 1001
    }
}