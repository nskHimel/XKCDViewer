package com.himel.xkcdviewer.downloader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ImageDownloader {
    companion object {
        private const val memSize = 20 * 1024
        private val cache = object: LruCache<String, Bitmap>(memSize) {
            override fun sizeOf(key: String?, value: Bitmap?): Int {
                return (value?.allocationByteCount  ?: 0) / 1024
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: String?,
                oldValue: Bitmap?,
                newValue: Bitmap?
            ) {
                oldValue?.recycle()
            }
        }

        fun fetch(url: String): Bitmap? {
            var bitmap: Bitmap? = cache.get(url)

            if (bitmap != null && !bitmap.isRecycled)
                return bitmap

            try {
                val connection = URL(url).openConnection() as HttpsURLConnection
                bitmap = BitmapFactory.decodeStream(connection.inputStream)

                synchronized(cache) {
                    cache.put(url, bitmap)
                }
            } catch (e: IOException) {}

            return bitmap
        }
    }
}