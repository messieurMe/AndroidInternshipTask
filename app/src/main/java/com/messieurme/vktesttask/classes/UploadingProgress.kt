package com.messieurme.vktesttask.classes

import android.net.Uri
import android.provider.Settings
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.InputStream
import java.net.URL
import java.nio.ByteBuffer
import java.util.*

@Entity
data class UploadingProgress(
    @PrimaryKey var sessionID: Long = System.currentTimeMillis(),
    var url: String = "-",
    var uri: String,
    var fileSize: Long,
    var uploaded: Long = 0,
    var lastBytesRead: Int = 0,
    var description: String = "",
    var name: String = "videoName",
    var isFinished: Boolean = false,
    var lastSuccess: Boolean = true,
    var lastRequestResponseCode: Int = 200,
) {
    var progress: Int
        get() = (uploaded * 100L / fileSize).toInt()
        private set(value) {}
}