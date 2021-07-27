package com.messieurme.vktesttask.classes

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.net.URISyntaxException

//That's not my class. I found it in internet. That's why I didn't change it
class UrlFromUri {
    companion object {

        @SuppressLint("NewApi")
        @Throws(URISyntaxException::class)
        fun getFilePath(context: Context, uri: Uri): String? {
            var uri: Uri = uri
            var selection: String? = null
            var selectionArgs: Array<String>? = null
            // Uri is different in versions after KITKAT (Android 4.4), we need to
            if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(
                    context.applicationContext,
                    uri
                )
            ) {
                when {
                    isExternalStorageDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":".toRegex()).toTypedArray()
                        return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }
                    isDownloadsDocument(uri) -> {
                        val id = DocumentsContract.getDocumentId(uri)
                        uri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
                        )
                    }
                    isMediaDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":".toRegex()).toTypedArray()
                        val type = split[0]
                        when (type) {
                            "image" -> uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }
                        selection = "_id=?"
                        selectionArgs = arrayOf(
                            split[1]
                        )
                    }
                }
            }
            if ("content".equals(uri.scheme, ignoreCase = true)) {
                if (isGooglePhotosUri(uri)) {
                    return uri.lastPathSegment
                }
                val projection = arrayOf(
                    MediaStore.Images.Media.DATA
                )
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver
                        .query(uri, projection, selection, selectionArgs, null)
                    val column_index: Int = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    if (cursor.moveToFirst()) {
                        return cursor.getString(column_index)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
            return null
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        private fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }
    }
}