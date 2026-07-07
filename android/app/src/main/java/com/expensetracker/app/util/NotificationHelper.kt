package com.expensetracker.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.expensetracker.app.R
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_SUBSCRIPTIONS = "subscriptions"
    private const val CHANNEL_BUDGETS = "budgets"
    private const val CHANNEL_AUTO_ENTRY = "auto_entry"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val subscriptionChannel = NotificationChannel(
                CHANNEL_SUBSCRIPTIONS,
                "订阅提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "订阅到期和续费提醒"
            }
            manager.createNotificationChannel(subscriptionChannel)

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGETS,
                "预算提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "预算超支和临近阈值提醒"
            }
            manager.createNotificationChannel(budgetChannel)

            val autoEntryChannel = NotificationChannel(
                CHANNEL_AUTO_ENTRY,
                "自动记账",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "自动生成的交易记录提醒"
            }
            manager.createNotificationChannel(autoEntryChannel)
        }
    }

    fun showSubscriptionReminder(context: Context, subscriptionName: String, daysLeft: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_SUBSCRIPTIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("订阅即将到期")
            .setContentText("${subscriptionName} 将在 ${daysLeft} 天后续费，请确认是否继续")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(subscriptionName.hashCode(), notification)
    }

    fun showBudgetAlert(context: Context, categoryName: String, percentUsed: Double) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val formatter = NumberFormat.getPercentInstance(Locale.CHINA)
        val percentStr = formatter.format(percentUsed / 100.0)

        val title = if (percentUsed >= 100.0) {
            "预算已超支"
        } else {
            "预算接近上限"
        }
        val text = "${categoryName} 预算已使用 ${percentStr}"

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGETS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(categoryName.hashCode(), notification)
    }

    fun showAutoEntrySummary(context: Context, count: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_AUTO_ENTRY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("自动记账完成")
            .setContentText("已自动生成 ${count} 条交易记录，请前往审核确认")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify("auto_entry".hashCode(), notification)
    }
}
