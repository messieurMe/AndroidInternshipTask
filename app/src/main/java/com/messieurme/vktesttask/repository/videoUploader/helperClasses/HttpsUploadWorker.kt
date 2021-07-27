package com.messieurme.vktesttask.repository.videoUploader.helperClasses

import com.messieurme.vktesttask.classes.UploadingItem
import com.messieurme.vktesttask.retrofit.Video
import java.io.DataOutputStream
import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection
import kotlin.math.min

class HttpsUploadWorker @Inject constructor() {

    private fun getHttpsUrlConnection(
        url: URL,
        uploaded: Long,
        fileSize: Long,
        batchSize: Long,
        sessionID: Long
    ) =
        (url.openConnection() as HttpsURLConnection).apply {
            doOutput = true
            useCaches = false
            requestMethod = "POST"

            setRequestProperty("Session-ID", "$sessionID")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Content-Length", "$batchSize")
            setRequestProperty("Content-Disposition", "attachment; filename=\"another_video$sessionID.mp4\"")
            setRequestProperty("Content-Range", "bytes $uploaded-${uploaded + batchSize - 1}/$fileSize")
            setRequestProperty("Content-Type", "application/octet-stream")
        }

    fun uploadFunction(uploadInfo: UploadingItem, buffer: ByteArray) {
        val connection: HttpsURLConnection =
            getHttpsUrlConnection(
                URL(uploadInfo.url),
                uploadInfo.uploaded,
                uploadInfo.fileSize,
                min(buffer.size.toLong(), uploadInfo.fileSize - uploadInfo.uploaded),
                uploadInfo.sessionID
            )
        try {
            DataOutputStream(connection.outputStream).use { dataOutputStream ->
                dataOutputStream.write(buffer, 0, uploadInfo.lastBytesRead)
                dataOutputStream.flush()
            }
            connection.inputStream.close()
            connection.disconnect()
            if (connection.responseCode in 200..201) {
                uploadInfo.uploaded += uploadInfo.lastBytesRead
                uploadInfo.lastSuccess = true
            } else {
                uploadInfo.lastSuccess = false
            }
        } catch (e: Exception) {
            uploadInfo.lastSuccess = false
        }
    }
}