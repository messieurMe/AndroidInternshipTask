package com.messieurme.vktesttask.classes

import android.net.Uri
import android.provider.Settings
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.InputStream
import java.net.URL
import java.nio.ByteBuffer
import java.util.*

@Entity
class UploadingProgress(
    @PrimaryKey var sessionID: Long = System.currentTimeMillis(),
    var url: String,
    var uri: String,
    var fileSize: Long,
    var uploaded: Long = 0,
    var lastBytesRead: Int = 0,
    var name: String = "videoName",
    var isFinished: Boolean = false,
    var lastSuccess: Boolean = true,
    var lastRequestResponseCode: Int = 200,
    var buffer: ByteArray = ByteArray((1024.0 * 1024.0 * 1.5).toInt()) // 1024 * 1024 * 1.5 == 1.5MB
) {
}