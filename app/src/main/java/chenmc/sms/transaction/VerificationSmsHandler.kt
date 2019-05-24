package chenmc.sms.transaction

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import chenmc.sms.code.helper.R
import chenmc.sms.data.VerificationCodeSms
import chenmc.sms.data.storage.AppPreference
import chenmc.sms.transaction.service.CopyTextService
import chenmc.sms.util.ToastUtil
import java.util.*


/**
 * @author Carter
 * Created on 2018-02-06
 */
class VerificationSmsHandler : ISmsHandler {
    private lateinit var context: Context

    override fun handle(context: Context, sms: String): Boolean {
        this.context = context.applicationContext

        val codeSms = SmsAnalyzer(context).analyseVerificationSms(sms) ?: return false

        handleCode(codeSms)
        return true
    }

    // 处理验证码
    private fun handleCode(codeSms: VerificationCodeSms) {

        var valuesSet: MutableSet<String> = HashSet(2)
        Collections.addAll(
            valuesSet,
            *context.resources.getStringArray(R.array.pref_def_values_sms_handle_ways)
        )
        val entryValues = context.resources.getStringArray(R.array.pref_entry_values_sms_handle_ways)
        valuesSet = AppPreference.smsHandleWays
        // 应用开启了自动复制验证码
        if (valuesSet.contains(entryValues[0])) {
            // 获取系统剪切板
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.primaryClip = ClipData.newPlainText("code", codeSms.code)
            ToastUtil.showSingletonToast(
                context.getString(R.string.sms_code_have_been_copied, codeSms.code),
                Toast.LENGTH_LONG
            )
        }
        // 应用开启了通知栏显示，在通知栏显示验证码
        if (valuesSet.contains(entryValues[1])) {
            notifyNotification(codeSms)
        }
    }

    // 在通知栏显示验证码和服务商
    private fun notifyNotification(codeSms: VerificationCodeSms) {
        val notificationId = System.currentTimeMillis().toInt()
        // 通知标题
        val title = context.getString(
            R.string.code_is,
            codeSms.serviceProvider
        )

        val contentIntent = PendingIntent.getService(
            context, notificationId,
            Intent(context, CopyTextService::class.java)
                .putExtra(CopyTextService.EXTRA_VERIFICATION, codeSms.code),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, NotificationContract.CHANNEL_ID_VERIFICATION)
            .setContentTitle(title)
            .setContentText(codeSms.code)
            .setTicker(title)
            .setContentIntent(contentIntent)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.ic_notification)
                .color = ContextCompat.getColor(context, R.color.colorPrimary)
        } else {
            builder.setSmallIcon(R.mipmap.ic_launcher)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}