package com.messieurme.vktesttask.classes

import com.google.gson.GsonBuilder
import com.messieurme.vktesttask.retrofit.Video
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.DataOutputStream
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.min


//Global functions for use
class SharedFunctions {
    companion object {
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
                setRequestProperty(
                    "Content-Disposition",
                    "attachment; filename=\"another_video$sessionID.mp4\""
                )
                setRequestProperty(
                    "Content-Range",
                    "bytes $uploaded-${uploaded + batchSize - 1}/$fileSize"
                )
                setRequestProperty("Content-Type", "application/octet-stream")
            }

        fun getProgressInPercents(uploaded: Long, total: Long)= (uploaded * 100L / total).toInt()

        fun uploadFunction(uploadInfo: UploadingProgress, inputStream: InputStream?) {
            val connection: HttpsURLConnection =
                getHttpsUrlConnection(
                    URL(uploadInfo.url),
                    uploadInfo.uploaded,
                    uploadInfo.fileSize,
                    min(uploadInfo.buffer.size.toLong(), uploadInfo.fileSize - uploadInfo.uploaded),
                    uploadInfo.sessionID
                )
            try {
                DataOutputStream(connection.outputStream).use { dataOuputStream ->
//                inputStream?.copyTo(dataOuputStream, uploadInfo.buffer.size)
                    if (uploadInfo.lastSuccess) {
                        inputStream!!.read(uploadInfo.buffer).also { uploadInfo.lastBytesRead = it }
                    }
                    dataOuputStream.write(uploadInfo.buffer, 0, uploadInfo.lastBytesRead)
                    dataOuputStream.flush()
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
                println("--EXCEPTION--")
                uploadInfo.lastSuccess = false
            }
        }

        var retrofit: Video = Retrofit.Builder()
            .baseUrl("https://api.vk.com/method/")
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().setLenient().create()
                )
            ).build().create(Video::class.java)
    }
}