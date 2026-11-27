package app.ani

import android.app.Notification
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler

/** Foreground service (dataSync) that keeps downloads running while the app is closed. */
class AniDownloadService : DownloadService(
    FOREGROUND_ID, 1000L, CHANNEL, R.string.dl_channel, 0,
) {
    override fun getDownloadManager(): DownloadManager = Graph.downloadManager

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        DownloadNotificationHelper(this, CHANNEL).buildProgressNotification(
            this, android.R.drawable.stat_sys_download, null, null, downloads, notMetRequirements,
        )

    companion object {
        const val CHANNEL = "ani_downloads"
        const val FOREGROUND_ID = 4201
    }
}
